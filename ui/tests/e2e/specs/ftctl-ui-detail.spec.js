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

function normalizeList (value) {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value
  }
  if (Array.isArray(value.ftctlprotectionvolume)) {
    return value.ftctlprotectionvolume
  }
  return [value]
}

function artifactDir () {
  return optionalEnv('FTCTL_UI_DETAIL_ARTIFACT_DIR',
    path.resolve(__dirname, '../../../.local-dev/ftctl-cloud/artifacts/ha-rky/06-ui-detail-20260503'))
}

async function collectPanelEvidence (page, ftctlPanel, outputDir) {
  fs.mkdirSync(outputDir, { recursive: true })
  const evidence = {
    url: page.url(),
    text: await ftctlPanel.innerText(),
    links: await ftctlPanel.locator('a[href]').evaluateAll(anchors => {
      return anchors.map(anchor => ({
        text: anchor.innerText.trim(),
        href: anchor.getAttribute('href')
      }))
    }),
    buttons: await ftctlPanel.locator('.ftctl-tab__operations button').evaluateAll(buttons => {
      return buttons.map(button => ({
        text: button.innerText.trim(),
        disabled: button.disabled || button.classList.contains('ant-btn-disabled')
      }))
    })
  }

  fs.writeFileSync(path.join(outputDir, 'panel-text.txt'), evidence.text)
  fs.writeFileSync(path.join(outputDir, 'ui-evidence.json'), JSON.stringify(evidence, null, 2))
  await page.screenshot({ path: path.join(outputDir, 'ftctl-ui-detail.png'), fullPage: true })
  return evidence
}

function findButton (buttons, pattern) {
  return buttons.find(button => pattern.test(button.text))
}

test.describe('FTCTL UI detail', () => {
  test('shows HA protection detail links states and actions', async ({ page }) => {
    test.setTimeout(120000)

    const username = requireEnv('FTCTL_UI_USERNAME')
    const password = requireEnv('FTCTL_UI_PASSWORD')
    const vmId = requireEnv('FTCTL_UI_VM_ID')
    const skipLogin = optionalEnv('FTCTL_UI_SKIP_LOGIN', 'false') === 'true'
    const expectedStandbyVmName = optionalEnv('FTCTL_UI_SECONDARY_VM_NAME', 'r9-01-standby')
    const expectedPeerHost = optionalEnv('FTCTL_UI_EXPECTED_PEER_HOST', 'ablecube22-1')
    const outputDir = artifactDir()

    const driver = new FtctlUiDriver(page)
    if (!skipLogin) {
      await driver.login(username, password)
    } else {
      await page.goto('/')
    }

    const { consoleErrors, apiFailures } = installUiErrorCollectors(page)

    const protectionResponsePromise = waitForJsonResponse(page, 'getFtctlProtection')
    const checkResponsePromise = waitForJsonResponse(page, 'getFtctlCheck')
    const healthResponsePromise = waitForJsonResponse(page, 'getFtctlHealth')
    const eventsResponsePromise = waitForJsonResponse(page, 'getFtctlEvents')

    await driver.openVmDetailById(vmId)
    await driver.openFtctlTab()

    const [protectionResponse, checkResponse, healthResponse, eventsResponse] = await Promise.all([
      protectionResponsePromise,
      checkResponsePromise,
      healthResponsePromise,
      eventsResponsePromise
    ])

    const protection = extractPayload(protectionResponse, 'getftctlprotectionresponse', 'ftctlprotection')
    const check = extractPayload(checkResponse, 'getftctlcheckresponse', 'ftctlcheck')
    const health = extractPayload(healthResponse, 'getftctlhealthresponse', 'ftctlhealth')
    const events = extractPayload(eventsResponse, 'getftctleventsresponse', 'ftctlevents')
    const volumes = normalizeList(protection.secondaryvolumes)

    fs.mkdirSync(outputDir, { recursive: true })
    fs.writeFileSync(path.join(outputDir, 'getFtctlProtection.json'), JSON.stringify(protectionResponse, null, 2))
    fs.writeFileSync(path.join(outputDir, 'getFtctlCheck.json'), JSON.stringify(checkResponse, null, 2))
    fs.writeFileSync(path.join(outputDir, 'getFtctlHealth.json'), JSON.stringify(healthResponse, null, 2))
    fs.writeFileSync(path.join(outputDir, 'getFtctlEvents.json'), JSON.stringify(eventsResponse, null, 2))

    expect(protection.enabled).toBe('true')
    expect(protection.mode).toBe('ha')
    expect(protection.backendmode).toBe('shared-blockcopy')
    expect(protection.provisioningbackend).toBe('cloud-managed')
    expect(protection.provisioningstate).toBe('Ready')
    expect(protection.protectionstate).toBe('syncing')
    expect(protection.transportstate).toBe('copying')
    expect(protection.activeside).toBe('primary')
    expect(protection.adminstate).toBe('active')
    expect(protection.fencingstate).toBe('clear')
    expect(protection.lasterror || '').toBe('')
    expect(protection.peerhostname).toBe(expectedPeerHost)
    expect(protection.secondaryvirtualmachinedisplayname).toBe(expectedStandbyVmName)
    expect(protection.secondaryvirtualmachineuuid).toBeTruthy()
    expect(volumes).toHaveLength(2)
    expect(check.result).toBe('ok')
    expect(check.inventoryresult).toBe('ok')
    expect(health.result).toBe('ok')
    expect(health.hostname).toBe('ablecube22-3')
    expect(events.events || []).not.toHaveLength(0)

    const ftctlPanel = page.locator('.ftctl-tab')
    await expect(ftctlPanel).toBeVisible({ timeout: 30000 })
    await expect(ftctlPanel.getByText(/보호 상세|Protection Details/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/점검|Check/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/상태|Health/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/이벤트|Events/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/HA/i, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/shared-blockcopy/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/cloud-managed/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/Ready/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/manual-block/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(expectedPeerHost, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(expectedStandbyVmName, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/syncing/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/copying/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/primary/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/active/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/clear/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/^OK$/, { exact: true }).first()).toBeVisible()

    for (const volume of volumes) {
      await expect(ftctlPanel.getByRole('link', { name: volume.name })).toBeVisible()
    }

    const evidence = await collectPanelEvidence(page, ftctlPanel, outputDir)
    expect(evidence.text).not.toMatch(/Primary RC\s+[0-9]/i)
    expect(evidence.text).not.toMatch(/Peer RC\s+[0-9]/i)
    expect(evidence.text).not.toMatch(/피어 호스트\s+[0-9]+/)

    const standbyLink = evidence.links.find(link => link.text === expectedStandbyVmName)
    expect(standbyLink).toBeTruthy()
    expect(standbyLink.href).toContain(`/vm/${protection.secondaryvirtualmachineuuid}`)

    for (const volume of volumes) {
      const volumeLink = evidence.links.find(link => link.text === volume.name)
      expect(volumeLink, `Missing secondary volume link for ${volume.name}`).toBeTruthy()
      expect(volumeLink.href).toContain(`/volume/${volume.id}`)
    }

    const pause = findButton(evidence.buttons, /일시 중지|Pause/)
    const resume = findButton(evidence.buttons, /재개|Resume/)
    const failover = findButton(evidence.buttons, /페일오버|Failover/)
    const failback = findButton(evidence.buttons, /페일백|Failback/)
    const confirmFence = findButton(evidence.buttons, /펜스 확인|Confirm Fence/)
    const clearFence = findButton(evidence.buttons, /펜스 해제|Clear Fence/)

    expect(pause).toMatchObject({ disabled: false })
    expect(resume).toMatchObject({ disabled: true })
    expect(failover).toMatchObject({ disabled: false })
    expect(failback).toMatchObject({ disabled: true })
    expect(confirmFence).toMatchObject({ disabled: true })
    expect(clearFence).toMatchObject({ disabled: true })

    expect(apiFailures, `API failures:\n${apiFailures.join('\n')}`).toEqual([])
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([])
  })
})
