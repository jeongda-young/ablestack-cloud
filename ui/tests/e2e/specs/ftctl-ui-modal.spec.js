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

async function selectByControlId (page, controlId, optionText) {
  await page.locator(`#${controlId}`).locator('..').locator('..').click()
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last()
  await expect(dropdown).toBeVisible()
  await dropdown.locator('.ant-select-item-option')
    .filter({ hasText: new RegExp(`^${optionText}$`) })
    .first()
    .click()
}

async function selectFirstOptionByControlId (page, controlId) {
  await page.locator(`#${controlId}`).locator('..').locator('..').click()
  const dropdown = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').last()
  await expect(dropdown).toBeVisible()
  const option = dropdown.locator('.ant-select-item-option:not(.ant-select-item-option-disabled)').first()
  await expect(option).toBeVisible()
  const text = (await option.innerText()).trim()
  await option.click()
  return text
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

test.describe('FTCTL protection dialog', () => {
  test('validates HA DR FT mode fields and peer-driven storage calls', async ({ page }) => {
    const username = requireEnv('FTCTL_UI_USERNAME')
    const password = requireEnv('FTCTL_UI_PASSWORD')
    const vmName = optionalEnv('FTCTL_UI_VM_NAME')
    const vmId = optionalEnv('FTCTL_UI_VM_ID')
    const skipLogin = optionalEnv('FTCTL_UI_SKIP_LOGIN', 'false') === 'true'
    if (!vmId && !vmName) {
      throw new Error('Either FTCTL_UI_VM_ID or FTCTL_UI_VM_NAME is required')
    }

    const driver = new FtctlUiDriver(page)
    if (!skipLogin) {
      await driver.login(username, password)
    } else {
      await page.goto('/')
    }

    const { consoleErrors, apiFailures } = installUiErrorCollectors(page)
    const storagePoolRequests = []
    const hostRequests = []
    page.on('request', request => {
      const url = request.url()
      if (!url.includes('/client/api')) {
        return
      }
      if (url.includes('command=listStoragePools')) {
        storagePoolRequests.push(url)
      }
      if (url.includes('command=listHosts')) {
        hostRequests.push(url)
      }
    })

    if (vmId) {
      await driver.openVmDetailById(vmId)
    } else {
      await driver.openVmByText(vmName)
    }
    await driver.openFtctlTab()
    await driver.openProtectionDialog()
    await expect(page.getByRole('dialog')).toBeVisible()

    await expect.poll(async () => hostRequests.length, { timeout: 15000 }).toBeGreaterThan(0)
    await expect.poll(async () => storagePoolRequests.length, { timeout: 15000 }).toBeGreaterThan(0)

    await expect.poll(() => getVisibleFormControlIds(page), { timeout: 15000 }).toEqual([
      'form_item_mode',
      'form_item_peerhostid',
      'form_item_targetstoragepoolid',
      'form_item_secondaryvmname',
      'form_item_backendmode',
      'form_item_fencingpolicy'
    ])

    await selectByControlId(page, 'form_item_mode', 'DR')
    await expect.poll(() => getVisibleFormControlIds(page), { timeout: 15000 }).toEqual([
      'form_item_mode',
      'form_item_peerhostid',
      'form_item_targetstoragepoolid',
      'form_item_secondaryvmname',
      'form_item_backendmode',
      'form_item_fencingpolicy'
    ])

    await selectByControlId(page, 'form_item_mode', 'FT')
    await expect.poll(() => getVisibleFormControlIds(page), { timeout: 15000 }).toEqual([
      'form_item_mode',
      'form_item_peerhostid',
      'form_item_targetstoragepoolid',
      'form_item_secondaryvmname',
      'form_item_manualxcoloendpoints'
    ])
    await expect(page.locator('#form_item_backendmode')).toHaveCount(0)
    await expect(page.locator('#form_item_fencingpolicy')).toHaveCount(0)

    const storageRequestsBeforePeer = storagePoolRequests.length
    const selectedPeerHost = await selectFirstOptionByControlId(page, 'form_item_peerhostid')
    expect(selectedPeerHost.length).toBeGreaterThan(0)
    await expect.poll(async () => storagePoolRequests.length, { timeout: 15000 }).toBeGreaterThan(storageRequestsBeforePeer)

    const storageRequestsBeforePool = storagePoolRequests.length
    const selectedStoragePool = await selectFirstOptionByControlId(page, 'form_item_targetstoragepoolid')
    expect(selectedStoragePool.length).toBeGreaterThan(0)
    await page.waitForTimeout(500)
    expect(storagePoolRequests.length).toBeGreaterThanOrEqual(storageRequestsBeforePool)

    await expect(page.getByText(/tcp:/, { exact: false }).first()).toBeVisible()
    await expect(page.getByText(/9000/, { exact: false }).first()).toBeVisible()
    await expect(page.getByText(/10809/, { exact: false }).first()).toBeVisible()
    await expect(page.getByText(/9998/, { exact: false }).first()).toBeVisible()

    const latestStoragePoolRequest = storagePoolRequests[storagePoolRequests.length - 1]
    expect(latestStoragePoolRequest).toContain('page=1')
    expect(latestStoragePoolRequest).toMatch(/pagesize=(500|20)/)
    expect(latestStoragePoolRequest).toMatch(/(hostid|clusterid)=/)

    await driver.closeProtectionDialog()
    await expect(page.getByRole('dialog')).toBeHidden()

    expect(apiFailures, `API failures:\n${apiFailures.join('\n')}`).toEqual([])
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([])
  })
})
