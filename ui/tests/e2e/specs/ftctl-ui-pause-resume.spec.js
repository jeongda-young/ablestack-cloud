// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

const fs = require('fs')
const path = require('path')
const { test, expect } = require('@playwright/test')
const { requireEnv, optionalEnv } = require('../helpers/env')
const FtctlUiDriver = require('../helpers/ftctl-ui-driver')

const REFRESH_TEXT = /\uC5C5\uB370\uC774\uD2B8|\uC0C8\uB85C\uACE0\uCE68|Refresh/
const PAUSE_TEXT = /\uC77C\uC2DC\s*\uC911\uC9C0|Pause/
const RESUME_TEXT = /\uC7AC\uAC1C|Resume/
const FAILOVER_TEXT = /\uD398\uC77C\uC624\uBC84|Failover/

function installUiErrorCollectors (page) {
  const consoleErrors = []
  const apiFailures = []
  page.on('console', message => {
    if (message.type() === 'error') {
      const location = message.location()
      consoleErrors.push(`${message.text()} ${location.url || ''}:${location.lineNumber || 0}`)
    }
  })
  page.on('response', response => {
    const url = response.url()
    if (url.includes('/client/api') && response.status() >= 400) {
      apiFailures.push(`${response.status()} ${url}`)
    }
  })
  return { consoleErrors, apiFailures }
}

function isApiCommandResponse (response, command) {
  const url = response.url()
  const postData = response.request().postData() || ''
  return url.includes('/client/api') &&
    response.status() === 200 &&
    (url.includes(`command=${command}`) || postData.includes(`command=${command}`))
}

async function waitForJsonResponse (page, command, timeout = 60000) {
  const response = await page.waitForResponse(candidate => isApiCommandResponse(candidate, command), { timeout })
  return JSON.parse(await response.text())
}

function extractPayload (response, responseKey, objectKey) {
  const payload = response?.[responseKey] || response || {}
  const value = payload?.[objectKey] || payload
  return Array.isArray(value) ? (value[0] || {}) : (value || {})
}

function extractActionPayload (response, command) {
  const responseKey = `${command.toLowerCase()}response`
  return extractPayload(response, responseKey, 'ftctlaction')
}

function artifactDir () {
  return optionalEnv('FTCTL_UI_PAUSE_RESUME_ARTIFACT_DIR',
    path.resolve(__dirname, '../../../.local-dev/ftctl-cloud/artifacts/ha-rky/07-pause-resume-20260503'))
}

function writeJson (outputDir, name, value) {
  fs.mkdirSync(outputDir, { recursive: true })
  fs.writeFileSync(path.join(outputDir, name), JSON.stringify(value, null, 2))
}

async function refreshAndCollect (page, outputDir, prefix) {
  const protectionPromise = waitForJsonResponse(page, 'getFtctlProtection')
  const checkPromise = waitForJsonResponse(page, 'getFtctlCheck')
  const healthPromise = waitForJsonResponse(page, 'getFtctlHealth')
  const eventsPromise = waitForJsonResponse(page, 'getFtctlEvents')

  const refreshButton = page.getByRole('button', { name: REFRESH_TEXT }).last()
  await expect(refreshButton).toBeEnabled({ timeout: 60000 })
  await refreshButton.click()

  const [protectionResponse, checkResponse, healthResponse, eventsResponse] = await Promise.all([
    protectionPromise,
    checkPromise,
    healthPromise,
    eventsPromise
  ])

  writeJson(outputDir, `${prefix}-getFtctlProtection.json`, protectionResponse)
  writeJson(outputDir, `${prefix}-getFtctlCheck.json`, checkResponse)
  writeJson(outputDir, `${prefix}-getFtctlHealth.json`, healthResponse)
  writeJson(outputDir, `${prefix}-getFtctlEvents.json`, eventsResponse)

  return {
    protection: extractPayload(protectionResponse, 'getftctlprotectionresponse', 'ftctlprotection'),
    check: extractPayload(checkResponse, 'getftctlcheckresponse', 'ftctlcheck'),
    health: extractPayload(healthResponse, 'getftctlhealthresponse', 'ftctlhealth'),
    events: extractPayload(eventsResponse, 'getftctleventsresponse', 'ftctlevents')
  }
}

async function collectPanelEvidence (page, outputDir, prefix) {
  const panel = page.locator('.ftctl-tab')
  await expect(panel).toBeVisible({ timeout: 30000 })
  const evidence = {
    url: page.url(),
    text: await panel.innerText(),
    buttons: await panel.locator('.ftctl-tab__operations button').evaluateAll(buttons => {
      return buttons.map(button => ({
        text: button.innerText.trim(),
        disabled: button.disabled || button.classList.contains('ant-btn-disabled')
      }))
    })
  }
  fs.mkdirSync(outputDir, { recursive: true })
  fs.writeFileSync(path.join(outputDir, `${prefix}-panel-text.txt`), evidence.text)
  fs.writeFileSync(path.join(outputDir, `${prefix}-ui-evidence.json`), JSON.stringify(evidence, null, 2))
  await page.screenshot({ path: path.join(outputDir, `${prefix}-ftctl-ui.png`), fullPage: true })
  return evidence
}

function findButtonState (evidence, pattern) {
  return evidence.buttons.find(button => pattern.test(button.text))
}

function expectOptionalOkResult (action) {
  if (action.result !== undefined && action.result !== null && action.result !== '') {
    expect(action.result).toBe('ok')
  }
}

async function clickActionAndCollectResponse (page, buttonPattern, command, outputDir, prefix) {
  const responsePromise = waitForJsonResponse(page, command, 120000)
  await page.locator('.ftctl-tab__operations').getByRole('button', { name: buttonPattern }).click()
  const response = await responsePromise
  writeJson(outputDir, `${prefix}-${command}.json`, response)
  return extractActionPayload(response, command)
}

test.describe('FTCTL UI pause/resume', () => {
  test('pauses and resumes HA protection from the Fault Protection tab', async ({ page }) => {
    test.setTimeout(180000)

    const username = requireEnv('FTCTL_UI_USERNAME')
    const password = requireEnv('FTCTL_UI_PASSWORD')
    const vmId = requireEnv('FTCTL_UI_VM_ID')
    const skipLogin = optionalEnv('FTCTL_UI_SKIP_LOGIN', 'false') === 'true'
    const outputDir = artifactDir()

    const driver = new FtctlUiDriver(page)
    if (!skipLogin) {
      await driver.login(username, password)
    } else {
      await page.goto('/')
    }

    const { consoleErrors, apiFailures } = installUiErrorCollectors(page)

    await driver.openVmDetailById(vmId)
    await driver.openFtctlTab()

    let before = await refreshAndCollect(page, outputDir, '01-before')
    if (before.protection.adminstate === 'paused') {
      const preconditionUi = await collectPanelEvidence(page, outputDir, '00-precondition-paused')
      expect(findButtonState(preconditionUi, RESUME_TEXT)).toMatchObject({ disabled: false })

      const preconditionResume = await clickActionAndCollectResponse(
        page, RESUME_TEXT, 'resumeFtctlProtection', outputDir, '00-precondition-resume')
      expect(preconditionResume.action).toMatch(/^RESUME/)
      expectOptionalOkResult(preconditionResume)
      expect(preconditionResume.exitcode).toBe(0)
      expect(preconditionResume.adminstate).toBe('active')

      before = await refreshAndCollect(page, outputDir, '01-before-after-precondition')
    }
    expect(before.protection.enabled).toBe('true')
    expect(before.protection.mode).toBe('ha')
    expect(before.protection.activeside).toBe('primary')
    expect(before.protection.adminstate).toBe('active')
    expect(before.protection.fencingstate).toBe('clear')
    expect(before.check.result).toBe('ok')
    expect(before.health.result).toBe('ok')

    const beforeUi = await collectPanelEvidence(page, outputDir, '01-before')
    expect(findButtonState(beforeUi, PAUSE_TEXT)).toMatchObject({ disabled: false })
    expect(findButtonState(beforeUi, RESUME_TEXT)).toMatchObject({ disabled: true })
    expect(findButtonState(beforeUi, FAILOVER_TEXT)).toMatchObject({ disabled: false })

    const pause = await clickActionAndCollectResponse(page, PAUSE_TEXT, 'pauseFtctlProtection', outputDir, '02-pause')
    expect(pause.action).toMatch(/^PAUSE/)
    expectOptionalOkResult(pause)
    expect(pause.exitcode).toBe(0)
    expect(pause.adminstate).toBe('paused')
    expect(pause.activeside).toBe('primary')
    expect(pause.fencingstate).toBe('clear')
    expect(pause.lasterror || '').toBe('')

    const paused = await refreshAndCollect(page, outputDir, '03-after-pause')
    expect(paused.protection.enabled).toBe('true')
    expect(paused.protection.adminstate).toBe('paused')
    expect(paused.protection.activeside).toBe('primary')
    expect(paused.protection.fencingstate).toBe('clear')
    expect(paused.protection.lasterror || '').toBe('')
    expect(paused.check.result).toBe('ok')
    expect(paused.health.result).toBe('ok')

    const pausedUi = await collectPanelEvidence(page, outputDir, '03-after-pause')
    expect(findButtonState(pausedUi, PAUSE_TEXT)).toMatchObject({ disabled: true })
    expect(findButtonState(pausedUi, RESUME_TEXT)).toMatchObject({ disabled: false })

    const resume = await clickActionAndCollectResponse(page, RESUME_TEXT, 'resumeFtctlProtection', outputDir, '04-resume')
    expect(resume.action).toMatch(/^RESUME/)
    expectOptionalOkResult(resume)
    expect(resume.exitcode).toBe(0)
    expect(resume.adminstate).toBe('active')
    expect(resume.activeside).toBe('primary')
    expect(resume.fencingstate).toBe('clear')
    expect(resume.lasterror || '').toBe('')

    const resumed = await refreshAndCollect(page, outputDir, '05-after-resume')
    expect(resumed.protection.enabled).toBe('true')
    expect(resumed.protection.adminstate).toBe('active')
    expect(resumed.protection.activeside).toBe('primary')
    expect(resumed.protection.fencingstate).toBe('clear')
    expect(resumed.protection.lasterror || '').toBe('')
    expect(resumed.check.result).toBe('ok')
    expect(resumed.health.result).toBe('ok')

    const resumedUi = await collectPanelEvidence(page, outputDir, '05-after-resume')
    expect(findButtonState(resumedUi, PAUSE_TEXT)).toMatchObject({ disabled: false })
    expect(findButtonState(resumedUi, RESUME_TEXT)).toMatchObject({ disabled: true })
    expect(findButtonState(resumedUi, FAILOVER_TEXT)).toMatchObject({ disabled: false })

    expect(apiFailures, `API failures:\n${apiFailures.join('\n')}`).toEqual([])
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([])
  })
})
