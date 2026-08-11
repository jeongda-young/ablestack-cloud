#!/usr/bin/env node

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

const CONFIG_PATH = path.resolve(process.argv[2] || path.join(__dirname, 'dist/config.json'))
const TMP_PATH = CONFIG_PATH + '.tmp'
const RELEASE_VERSION_PATTERN = /^v\d+\.\d+\.\d+-(Europa|Diplo)-\d{8}(?:-(ALPHA|BETA|RC)\d+)?$/

function readJSON (file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'))
}

function writeJSONAtomic (file, obj) {
  const text = JSON.stringify(obj, null, 2) + '\n'
  fs.writeFileSync(TMP_PATH, text, 'utf8')
  fs.renameSync(TMP_PATH, file)
}

function getBuildVersion () {
  const buildVersion = (process.env.ABLESTACK_UI_BUILD_VERSION || '').trim()
  if (!buildVersion) return null
  if (!RELEASE_VERSION_PATTERN.test(buildVersion)) {
    throw new Error(`Invalid ABLESTACK_UI_BUILD_VERSION: ${buildVersion}`)
  }
  return buildVersion
}

const data = readJSON(CONFIG_PATH)
const buildVersion = getBuildVersion()
if (buildVersion) {
  data.buildVersion = buildVersion
  writeJSONAtomic(CONFIG_PATH, data)
  console.log('[OK] buildVersion:', data.buildVersion)
} else {
  console.log('[SKIP] ABLESTACK_UI_BUILD_VERSION is not set; keeping buildVersion:', data.buildVersion)
}
