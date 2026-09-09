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

const selectors = {
  login: {
    username: 'input[type="text"], input[name="username"], input[name="email"]',
    password: 'input[type="password"]',
    submit: 'button[type="submit"]'
  },
  vm: {
    search: 'input[placeholder*="Search"], input[type="search"]',
    ftctlTabText: /\uC7A5\uC560\uBCF4\uD638|Fault Protection|FTCTL/
  },
  ftctl: {
    refreshButtonText: /\uC5C5\uB370\uC774\uD2B8|\uC0C8\uB85C\uACE0\uCE68|Refresh/,
    protectionButtonText: /\uBCF4\uD638\s*\uC124\uC815|Protection/,
    cancelButtonText: /\uCDE8\uC18C|Cancel/,
    notConfiguredText: /\uBCF4\uD638\s*\uC124\uC815\uC774\s*\uB418\uC5B4\s*\uC788\uC9C0\s*\uC54A\uC74C|Protection is not configured/,
    registerActionText: 'registerFtctlProtection',
    pauseActionText: 'pauseFtctlProtection',
    failoverActionText: 'failoverFtctlProtection',
    eventTableText: 'Details'
  }
}

module.exports = selectors
