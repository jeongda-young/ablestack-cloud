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

const path = require('path')
const { defineConfig, devices } = require('@playwright/test')

const artifactBase = process.env.FTCTL_UI_E2E_ARTIFACT_DIR ||
  path.resolve(__dirname, '../../../.local-dev/ftctl-cloud/artifacts/ui-e2e')

module.exports = defineConfig({
  testDir: path.resolve(__dirname, './specs'),
  timeout: 120000,
  expect: {
    timeout: 15000
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: path.join(artifactBase, 'playwright-report'), open: 'never' }],
    ['json', { outputFile: path.join(artifactBase, 'summary.json') }]
  ],
  outputDir: path.join(artifactBase, 'test-results'),
  use: {
    baseURL: process.env.FTCTL_UI_BASE_URL || 'http://127.0.0.1:8080/client',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    ignoreHTTPSErrors: process.env.FTCTL_UI_IGNORE_HTTPS_ERRORS === 'true',
    headless: process.env.FTCTL_UI_HEADED !== 'true',
    storageState: process.env.FTCTL_UI_STORAGE_STATE || undefined
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 1024 }
      }
    }
  ]
})
