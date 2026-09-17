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

import { startVolumeOperation, volumeActionReason, clearVolumeOperations } from '@/utils/vmVolumeActions'
const flush = async () => { for (let i = 0; i < 60; i++) await Promise.resolve() }
const vm = { id: 'vm', state: 'Running', zoneid: 'zone', account: 'account', domainid: 'domain' }
const volume = { id: 'volume', type: 'DATADISK', state: 'Ready', zoneid: 'zone', account: 'account', domainid: 'domain' }
function dependencies () {
  return { current: () => true, refresh: jest.fn(), validate: jest.fn().mockResolvedValue(), submit: jest.fn().mockImplementation(operation => Promise.resolve({ [operation.steps[operation.stage].toLowerCase() + 'response']: { jobid: 'job-' + operation.stage } })), poll: jest.fn().mockResolvedValue({ jobstatus: 1 }) }
}
beforeEach(clearVolumeOperations)
test('rejects root, shared, flattening, wrong owner and changed attachment', () => {
  expect(volumeActionReason('detachVolume', { ...volume, type: 'ROOT', virtualmachineid: vm.id }, vm)).toBeTruthy()
  expect(volumeActionReason('detachVolume', { ...volume, isshared: true }, vm)).toBeTruthy()
  expect(volumeActionReason('detachVolume', { ...volume, virtualmachineid: vm.id, clonefastflattenstatus: 'running' }, vm)).toBeTruthy()
  expect(volumeActionReason('attachVolume', { ...volume, account: 'other' }, vm)).toBeTruthy()
  expect(volumeActionReason('destroyVolume', { ...volume, virtualmachineid: 'other' }, vm)).toBeTruthy()
  expect(volumeActionReason('attachVolume', volume, vm)).toBe('')
})
test('detaches once and retries only failed destruction', async () => {
  const deps = dependencies(); deps.poll.mockResolvedValueOnce({ jobstatus: 1 }).mockResolvedValueOnce({ jobstatus: 2, jobresult: { errortext: 'busy' } }).mockResolvedValue({ jobstatus: 1 })
  const op = startVolumeOperation('key', { steps: ['detachVolume', 'destroyVolume'], volume }, deps)
  expect(startVolumeOperation('key', { steps: ['detachVolume'] }, deps)).toBe(op)
  await flush(); expect(op.stage).toBe(1); expect(op.status).toBe('failed')
  op.resume(); op.resume(); await flush()
  expect(op.status).toBe('complete'); expect(deps.submit).toHaveBeenCalledTimes(3)
})
test('unknown job polls same ID without resubmitting detach', async () => {
  const deps = dependencies(); deps.poll.mockResolvedValueOnce({ trackingStatus: 'unknown' }).mockResolvedValue({ jobstatus: 1 })
  const op = startVolumeOperation('key', { steps: ['detachVolume', 'destroyVolume'], volume }, deps)
  await flush(); expect(op.status).toBe('unknown'); expect(op.jobId).toBe('job-0')
  op.resume(); await flush(); expect(op.status).toBe('complete'); expect(deps.submit).toHaveBeenCalledTimes(2)
})
test('ambiguous submission never repeats mutation', async () => {
  const deps = dependencies(); deps.submit.mockRejectedValue(new Error('network lost'))
  const op = startVolumeOperation('key', { steps: ['detachVolume', 'destroyVolume'], volume }, deps)
  await flush(); op.resume(); await flush()
  expect(op.status).toBe('unknown'); expect(deps.submit).toHaveBeenCalledTimes(1)
})
test('security scope change stops next destructive step', async () => {
  let resolvePoll
  const deps = dependencies(); deps.poll.mockReturnValue(new Promise(resolve => { resolvePoll = resolve }))
  startVolumeOperation('key', { steps: ['detachVolume', 'destroyVolume'], volume }, deps)
  await flush(); clearVolumeOperations(); resolvePoll({ jobstatus: 1 }); await flush()
  expect(deps.submit).toHaveBeenCalledTimes(1)
})
test('created volume ID survives attach failure', async () => {
  const deps = dependencies(); deps.poll.mockResolvedValueOnce({ jobstatus: 1, jobresult: { volume } }).mockResolvedValueOnce({ jobstatus: 2 })
  const op = startVolumeOperation('key', { steps: ['createVolume', 'attachVolume'], values: { name: 'test' } }, deps)
  await flush(); expect(op.volume.id).toBe(volume.id); expect(op.stage).toBe(1); expect(op.status).toBe('failed')
})
