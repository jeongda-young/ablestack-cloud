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

test.describe('FTCTL UI smoke', () => {
  test('opens fault protection tab and protection dialog without UI errors', async ({ page }) => {
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

    if (vmId) {
      await driver.openVmDetailById(vmId)
    } else {
      await driver.openVmByText(vmName)
    }

    await driver.openFtctlTab()

    await expect(page.getByRole('button', { name: /\uBCF4\uD638\s*\uC124\uC815|Protection/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /\uC5C5\uB370\uC774\uD2B8|\uC0C8\uB85C\uACE0\uCE68|Refresh/ }).last()).toBeVisible()

    const configuredMarker = page.getByText(/Protection State|\uBCF4\uD638\s*\uC0C1\uD0DC|Transport State|\uC804\uC1A1\s*\uC0C1\uD0DC/, { exact: false }).first()
    const notConfiguredMarker = page.getByText(/\uBCF4\uD638\s*\uC124\uC815\uC774\s*\uB418\uC5B4\s*\uC788\uC9C0\s*\uC54A\uC74C|Protection is not configured/, { exact: false }).first()
    await expect(configuredMarker.or(notConfiguredMarker)).toBeVisible()

    await driver.openProtectionDialog()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/Mode|\uBAA8\uB4DC/, { exact: false }).first()).toBeVisible()
    await expect(page.getByText(/Peer Host ID|\uD53C\uC5B4\s*\uD638\uC2A4\uD2B8\s*ID/, { exact: false }).first()).toBeVisible()
    await driver.closeProtectionDialog()
    await expect(page.getByRole('dialog')).toBeHidden()

    expect(apiFailures, `API failures:\n${apiFailures.join('\n')}`).toEqual([])
    expect(consoleErrors, `Console errors:\n${consoleErrors.join('\n')}`).toEqual([])
  })
})
