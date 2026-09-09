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

const { test, expect } = require('@playwright/test')
const { requireEnv, optionalEnv } = require('../helpers/env')
const FtctlUiDriver = require('../helpers/ftctl-ui-driver')

async function selectByControlId (page, controlId, optionText) {
  await page.locator(`#${controlId}`).locator('..').locator('..').click()
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last()
  await expect(dropdown).toBeVisible()
  await dropdown.locator('.ant-select-item-option')
    .filter({ hasText: new RegExp(`^${optionText}$`) })
    .first()
    .click()
}

async function selectFirstOptionByControlId (page, controlId, preferredText = null) {
  await page.locator(`#${controlId}`).locator('..').locator('..').click()
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last()
  await expect(dropdown).toBeVisible()
  const options = dropdown.locator('.ant-select-item-option:not(.ant-select-item-option-disabled)')
  const option = preferredText
    ? options.filter({ hasText: new RegExp(preferredText) }).first()
    : options.first()
  await expect(option).toBeVisible()
  const text = (await option.innerText()).trim()
  await option.click()
  return text
}

async function getVisibleFormControlIds (page) {
  return page.locator('.ant-modal .ant-form-item').evaluateAll(items => {
    return items.map(item => {
      const control = item.querySelector('input[id^="form_item_"], textarea[id^="form_item_"]')
      if (!control) {
        return null
      }
      return control.id
    }).filter(Boolean)
  })
}

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
    (url.includes(`command=${command}`) || postData.includes(`command=${command}`))
}

function parseProtectionResponse (body) {
  const payload = JSON.parse(body)
  if (payload.getftctlprotectionresponse && payload.getftctlprotectionresponse.ftctlprotection) {
    return payload.getftctlprotectionresponse.ftctlprotection
  }
  if (payload.registerftctlprotectionresponse && payload.registerftctlprotectionresponse.ftctlprotection) {
    return payload.registerftctlprotectionresponse.ftctlprotection
  }
  if (payload.queryasyncjobresultresponse &&
    payload.queryasyncjobresultresponse.jobresult &&
    payload.queryasyncjobresultresponse.jobresult.ftctlprotection) {
    return payload.queryasyncjobresultresponse.jobresult.ftctlprotection
  }
  return undefined
}

function parseTimeoutEnv (name, defaultValue) {
  const value = Number.parseInt(optionalEnv(name, `${defaultValue}`), 10)
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`)
  }
  return value
}

test.describe('FTCTL protection save', () => {
  test('registers HA protection and refreshes status sections', async ({ page }) => {
    const username = requireEnv('FTCTL_UI_USERNAME')
    const password = requireEnv('FTCTL_UI_PASSWORD')
    const vmId = requireEnv('FTCTL_UI_VM_ID')
    const skipLogin = optionalEnv('FTCTL_UI_SKIP_LOGIN', 'false') === 'true'
    const preferredPeer = optionalEnv('FTCTL_UI_PEER_HOST_PATTERN', 'ablecube22-[12]')
    const preferredStorage = optionalEnv('FTCTL_UI_STORAGE_POOL_PATTERN', 'Primary Storage|RBD|ZONE')
    const expectedStorage = optionalEnv('FTCTL_UI_EXPECT_STORAGE_PATTERN', preferredStorage)
    const selectedBackendMode = optionalEnv('FTCTL_UI_BACKEND_MODE', 'shared-blockcopy')
    const expectedBackendMode = optionalEnv('FTCTL_UI_EXPECT_BACKEND_MODE', 'shared-blockcopy')
    const secondaryVmName = optionalEnv('FTCTL_UI_SECONDARY_VM_NAME', 'r9-01-standby')
    const registerTimeoutMs = parseTimeoutEnv('FTCTL_UI_REGISTER_TIMEOUT_MS', 180000)

    test.setTimeout(Math.max(test.info().timeout, registerTimeoutMs + 60000))

    const driver = new FtctlUiDriver(page)
    if (!skipLogin) {
      await driver.login(username, password)
    } else {
      await page.goto('/')
    }

    const { consoleErrors, apiFailures } = installUiErrorCollectors(page)
    await driver.openVmDetailById(vmId)
    await driver.openFtctlTab()
    await driver.openProtectionDialog()
    await expect(page.getByRole('dialog')).toBeVisible()

    await expect.poll(() => getVisibleFormControlIds(page), { timeout: 15000 }).toEqual([
      'form_item_mode',
      'form_item_peerhostid',
      'form_item_targetstoragepoolid',
      'form_item_secondaryvmname',
      'form_item_backendmode',
      'form_item_fencingpolicy'
    ])

    await selectByControlId(page, 'form_item_mode', 'HA')
    await selectFirstOptionByControlId(page, 'form_item_peerhostid', preferredPeer)
    await selectFirstOptionByControlId(page, 'form_item_targetstoragepoolid', preferredStorage)
    await page.locator('#form_item_secondaryvmname').fill(secondaryVmName)
    await selectByControlId(page, 'form_item_backendmode', selectedBackendMode)
    await selectByControlId(page, 'form_item_fencingpolicy', 'manual-block')

    const registerResponsePromise = page.waitForResponse(response =>
      isApiCommandResponse(response, 'registerFtctlProtection') && response.status() === 200,
    { timeout: registerTimeoutMs })
    await page.getByRole('dialog').getByRole('button', { name: /^\uD655\uC778$|^OK$/ }).click()
    const registerResponse = await registerResponsePromise
    const registerBody = await registerResponse.text()
    const protection = parseProtectionResponse(registerBody)

    await expect(page.getByRole('dialog')).toBeHidden({ timeout: 30000 })
    expect(protection, `Register response did not contain ftctlprotection: ${registerBody}`).toBeTruthy()
    expect(protection.enabled).toBe('true')
    expect(protection.mode).toBe('ha')
    expect(protection.backendmode).toBe(expectedBackendMode)
    expect(protection.provisioningbackend).toBe('cloud-managed')
    expect(protection.fencingpolicy).toBe('manual-block')
    expect(protection.targetstoragepoolname).toMatch(new RegExp(expectedStorage))
    expect(protection.lasterror || '', `FTCTL last error: ${protection.lasterror || ''}`).toBe('')
    expect(protection.protectionstate || '', `FTCTL protection state: ${protection.protectionstate || ''}`).not.toBe('error')
    expect(protection.transportstate || '', `FTCTL transport state: ${protection.transportstate || ''}`).not.toBe('failed')

    await driver.openFtctlTab()
    const ftctlPanel = page.getByRole('tabpanel', { name: /\uC7A5\uC560\uBCF4\uD638|Fault Protection/ })
    await expect(ftctlPanel.getByText(/Protection Details|\uBCF4\uD638 \uC0C1\uC138/, { exact: false }).first()).toBeVisible({ timeout: 30000 })
    await expect(ftctlPanel.getByText(/Check|\uC810\uAC80/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/Health|\uC0C1\uD0DC/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/Events|\uC774\uBCA4\uD2B8/, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/HA/i, { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(new RegExp(expectedBackendMode), { exact: false }).first()).toBeVisible()
    await expect(ftctlPanel.getByText(/manual-block/, { exact: false }).first()).toBeVisible()

    expect(apiFailures, `API failures:\n${apiFailures.join('\n')}`).toEqual([])
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([])
  })
})
