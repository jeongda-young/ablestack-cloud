// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import { shallowMount } from '@vue/test-utils'
import { getAPI } from '@/api'
import BackupStagingManagement from '@/views/storage/BackupStagingManagement.vue'

jest.mock('@/api', () => ({ getAPI: jest.fn(), postAPI: jest.fn() }))

async function mount (info, operation = 'BACKUP') {
  getAPI.mockResolvedValue({ getbackupstaginginforesponse: info })
  const wrapper = shallowMount(BackupStagingManagement, {
    props: { resource: { id: 'backup-1', status: 'Failed', stagingqueue: { operation } } },
    global: {
      mocks: {
        $store: { getters: { apis: { reconcileBackupStagingJob: {} } } },
        $t: key => key,
        $toLocaleDate: value => value
      }
    }
  })
  for (let i = 0; i < 5; i++) await Promise.resolve()
  return wrapper
}

beforeEach(() => jest.clearAllMocks())

test('completed staging and record cleanup with a released reservation cannot be retried', async () => {
  const wrapper = await mount({ hoststate: 'FAILED', cleanupstate: 'COMPLETED', jobcleanupstate: 'COMPLETED', stagingqueue: { state: 'RELEASED' } })
  expect(wrapper.vm.canCleanup).toBe(false)
  wrapper.unmount()
})

test('finished staging still allows confirmed pending Host record or reservation cleanup', async () => {
  for (const info of [{ jobcleanupstate: 'WAITING', stagingqueue: { state: 'RELEASED' } }, { jobcleanupstate: 'COMPLETED', stagingqueue: { state: 'ADMITTED' } }]) {
    const wrapper = await mount({ ...info, hoststate: 'UNKNOWN', startstate: 'STARTED', cleanupstate: 'COMPLETED' })
    expect(wrapper.vm.canCleanup).toBe(true)
    wrapper.unmount()
  }
})

test('a completed restore or historical attempt cannot acquire a cleanup action', async () => {
  for (const info of [{ cleanupstate: 'COMPLETED' }, { historical: true, cleanupstate: 'WAITING' }]) {
    const wrapper = await mount({ ...info, hoststate: 'FAILED', vmrestore: { outcome: 'RECOVERY_REQUIRED' } }, 'RESTORE')
    expect(wrapper.vm.canCleanup).toBe(false)
    wrapper.unmount()
  }
})
