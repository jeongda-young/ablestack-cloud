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

const selectors = require('./selectors')

class FtctlUiDriver {
  constructor (page) {
    this.page = page
  }

  async login (username, password) {
    await this.page.goto('/')
    await this.page.locator(selectors.login.username).first().fill(username)
    await this.page.locator(selectors.login.password).first().fill(password)
    await this.page.locator(selectors.login.submit).first().click()
    await this.page.waitForLoadState('networkidle')
  }

  async openVmByText (vmText) {
    await this.page.getByText(vmText, { exact: false }).first().click()
  }

  async openVmDetailById (vmId) {
    const baseUrl = process.env.FTCTL_UI_BASE_URL || ''
    const normalizedBaseUrl = baseUrl.replace(/\/$/, '')
    if (normalizedBaseUrl) {
      await this.page.goto(`${normalizedBaseUrl}/#/vm/${vmId}`)
    } else {
      await this.page.goto(`/#/vm/${vmId}`)
    }
    await this.page.waitForLoadState('networkidle')
  }

  async openFtctlTab () {
    await this.page.getByText(selectors.vm.ftctlTabText, { exact: false }).first().click()
    await this.page.waitForLoadState('networkidle')
  }

  async refreshFtctlTab () {
    await this.page.getByRole('button', { name: selectors.ftctl.refreshButtonText }).last().click()
  }

  async openProtectionDialog () {
    await this.page.getByRole('button', { name: selectors.ftctl.protectionButtonText }).click()
  }

  async closeProtectionDialog () {
    await this.page.getByRole('button', { name: selectors.ftctl.cancelButtonText }).click()
  }
}

module.exports = FtctlUiDriver
