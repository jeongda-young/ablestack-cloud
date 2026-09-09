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

import { flushPromises, shallowMount } from '@vue/test-utils'
import FtctlTab from '@/views/compute/FtctlTab'
import { getAPI, postAPI } from '@/api'
import eventBus from '@/config/eventBus'

jest.mock('@/api', () => ({
  getAPI: jest.fn(),
  postAPI: jest.fn()
}))

jest.mock('@/config/eventBus', () => ({
  __esModule: true,
  default: {
    emit: jest.fn()
  }
}))

const createWrapper = (apis = {}) => {
  return shallowMount(FtctlTab, {
    props: {
      resource: {
        id: 'vm-1',
        name: 'vm-name',
        hypervisor: 'KVM',
        vmtype: 'User'
      },
      loading: false
    },
    global: {
      mocks: {
        $store: {
          getters: {
            apis,
            userInfo: {
              roletype: 'Admin'
            }
          }
        },
        $message: {
          success: jest.fn(),
          error: jest.fn(),
          warning: jest.fn()
        },
        $pollJob: jest.fn(),
        $t: (key) => key
      },
      stubs: {
        'a-spin': { template: '<div><slot /></div>' },
        'a-space': { template: '<div><slot /></div>' },
        'a-popconfirm': { template: '<div><slot /></div>' },
        'a-modal': { template: '<div><slot /></div>' },
        'a-button': { template: '<button><slot /></button>' },
        'a-alert': { template: '<div><slot name="message" /></div>' },
        'a-card': { template: '<div><slot /></div>' },
        'a-tag': { template: '<span><slot /></span>' },
        'a-descriptions': { template: '<div><slot /></div>' },
        'a-descriptions-item': { template: '<div><slot /></div>' },
        'a-divider': true,
        'a-table': { template: '<div><slot /></div>' },
        SafetyCertificateOutlined: true,
        SyncOutlined: true,
        PauseCircleOutlined: true,
        PlayCircleOutlined: true,
        ThunderboltOutlined: true,
        UndoOutlined: true,
        CheckCircleOutlined: true,
        ClearOutlined: true
      }
    }
  })
}

describe('Views > compute > FtctlTab.vue', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    jest.spyOn(console, 'warn').mockImplementation(() => {})
    global.window.confirm = jest.fn(() => true)
  })

  it('fetches protection, check, health and events on create', async () => {
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: {
              ftctlprotection: {
                enabled: 'true',
                mode: 'dr',
                peerhostname: 'ablecube22-1',
                secondaryvmname: 'i-2-303-VM',
                secondaryvirtualmachineuuid: 'secondary-vm-uuid',
                secondaryvirtualmachinedisplayname: 'vm-name-secondary',
                secondaryvolumes: [
                  { id: 'secondary-root-volume-uuid', name: 'vm-name-secondary-root', path: 'rbd-root-path' },
                  { id: 'secondary-data-volume-uuid', name: 'vm-name-secondary-data', path: 'rbd-data-path' }
                ],
                targetstoragepoolname: 'Primary Storage',
                protectionstate: 'protected',
                transportstate: 'mirroring',
                activeside: 'primary',
                adminstate: 'running',
                fencingstate: 'clear'
              }
            }
          })
        case 'getFtctlCheck':
          return Promise.resolve({
            getftctlcheckresponse: {
              ftctlcheck: {
                result: 'ok',
                inventoryresult: 'healthy',
                primaryrc: 0,
                peerrc: 1
              }
            }
          })
        case 'getFtctlHealth':
          return Promise.resolve({
            getftctlhealthresponse: {
              ftctlhealth: {
                result: 'ok',
                hostid: 201,
                hostname: 'ablecube22-3',
                uri: 'qemu+ssh://10.0.0.11/system',
                rc: 0
              }
            }
          })
        case 'getFtctlEvents':
          return Promise.resolve({
            getftctleventsresponse: {
              ftctlevents: {
                events: [
                  { ts: '2026-04-19T00:10:00+09:00', event: 'older', result: 'ok' },
                  { ts: '2026-04-19T00:20:00+09:00', event: 'newer', result: 'warn', details: '{"reason":"backoff"}' }
                ]
              }
            }
          })
        default:
          return Promise.resolve({})
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      pauseFtctlProtection: true,
      resumeFtctlProtection: true,
      failoverFtctlProtection: true,
      failbackFtctlProtection: true,
      confirmFtctlFence: true,
      clearFtctlFence: true
    })

    await flushPromises()

    expect(getAPI).toHaveBeenCalledWith('getFtctlProtection', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlCheck', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlHealth', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlEvents', { virtualmachineid: 'vm-1', limit: 100 })
    expect(wrapper.vm.protection.mode).toBe('dr')
    expect(wrapper.vm.peerHostDisplay).toBe('ablecube22-1')
    expect(wrapper.vm.secondaryVmDisplay).toBe('vm-name-secondary')
    expect(wrapper.vm.secondaryVmRouteId).toBe('secondary-vm-uuid')
    expect(wrapper.vm.secondaryVolumeItems.map(volume => volume.name)).toEqual(['vm-name-secondary-root', 'vm-name-secondary-data'])
    expect(wrapper.vm.healthHostDisplay).toBe('ablecube22-3')
    expect(wrapper.vm.primaryExecutionState).toBe('Started')
    expect(wrapper.vm.peerExecutionState).toBe('Error')
    expect(wrapper.vm.checkResult.inventoryresult).toBe('healthy')
    expect(wrapper.vm.healthResult.uri).toBe('qemu+ssh://10.0.0.11/system')
    expect(wrapper.vm.events[0].event).toBe('newer')
    expect(wrapper.vm.events[0].timestamp).toBe('2026-04-19T00:20:00+09:00')
    expect(wrapper.vm.events[1].event).toBe('older')
    expect(wrapper.vm.canRunActions).toBe(true)
    expect(wrapper.vm.actionDefinitions.find(action => action.api === 'pauseFtctlProtection').disabled).toBe(false)
    expect(wrapper.vm.operationalSummary.type).toBe('warning')
  })

  it('loads read-only runtime data for standby protection view', async () => {
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: {
              ftctlprotection: {
                enabled: 'true',
                mode: 'ha',
                protectionrole: 'standby',
                primaryvirtualmachineid: 101,
                primaryvirtualmachineuuid: 'primary-vm-uuid',
                primaryvirtualmachinename: 'r9-01',
                secondaryvirtualmachineid: 308,
                secondaryvirtualmachineuuid: 'standby-vm-uuid',
                secondaryvirtualmachinedisplayname: 'r9-01-standby',
                protectionstate: 'failed_over',
                transportstate: 'failed_over',
                activeside: 'secondary',
                adminstate: 'active',
                fencingstate: 'manual-fenced'
              }
            }
          })
        case 'getFtctlCheck':
          return Promise.resolve({
            getftctlcheckresponse: {
              ftctlcheck: {
                virtualmachineid: 308,
                vmname: 'r9-01',
                result: 'ok',
                inventoryresult: 'healthy',
                primaryrc: 0,
                peerrc: 0
              }
            }
          })
        case 'getFtctlHealth':
          return Promise.resolve({
            getftctlhealthresponse: {
              ftctlhealth: {
                virtualmachineid: 308,
                result: 'ok',
                hostname: 'ablecube22-3',
                uri: 'qemu:///system',
                rc: 0
              }
            }
          })
        case 'getFtctlEvents':
          return Promise.resolve({
            getftctleventsresponse: {
              ftctlevents: {
                virtualmachineid: 308,
                vmname: 'r9-01',
                count: 1,
                events: [
                  { ts: '2026-05-03T13:47:10+09:00', stage: 'failover', event: 'failover.precheck', result: 'ok' }
                ]
              }
            }
          })
        default:
          return Promise.resolve({})
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      pauseFtctlProtection: true,
      resumeFtctlProtection: true,
      failoverFtctlProtection: true
    })

    await flushPromises()

    expect(getAPI).toHaveBeenCalledWith('getFtctlProtection', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlCheck', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlHealth', { virtualmachineid: 'vm-1' })
    expect(getAPI).toHaveBeenCalledWith('getFtctlEvents', { virtualmachineid: 'vm-1', limit: 100 })
    expect(wrapper.vm.standbyProtectionView).toBe(true)
    expect(wrapper.vm.canRunActions).toBe(false)
    expect(wrapper.vm.checkResult.vmname).toBe('r9-01')
    expect(wrapper.vm.healthHostDisplay).toBe('ablecube22-3')
    expect(wrapper.vm.events).toHaveLength(1)
    expect(wrapper.vm.events[0].event).toBe('failover.precheck')
    expect(wrapper.vm.operationalSummary.type).toBe('info')
    expect(wrapper.vm.stateTagColor('failed_over')).toBe('blue')
  })

  it('shows distinct replica recovery actions without duplicate release button', async () => {
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'dr',
              drpeersitetype: 'remote-mold',
              protectionrole: 'standby',
              protectionstate: 'failed_over',
              transportstate: 'failed_over',
              activeside: 'secondary',
              adminstate: 'active',
              fencingstate: 'clear',
              secondaryvirtualmachinestate: 'Running'
            }
          }
        })
      }
      return Promise.resolve({})
    })

    const wrapper = createWrapper({
      failbackFtctlDrReplica: true,
      adoptFtctlDrReplica: true
    })

    await flushPromises()

    expect(wrapper.vm.replicaRecoveryActionView).toBe(true)
    expect(wrapper.vm.canRunActions).toBe(true)
    expect(wrapper.vm.actionDefinitions.map(action => action.api)).toEqual([
      'failbackFtctlDrReplica',
      'adoptFtctlDrReplica'
    ])
    expect(wrapper.vm.actionDefinitions.find(action => action.api === 'releaseFtctlDrReplicaProtection')).toBeUndefined()

    const failbackAction = wrapper.vm.actionDefinitions.find(action => action.api === 'failbackFtctlDrReplica')
    expect(failbackAction.replicaFailbackModal).toBe(true)
    expect(failbackAction.disabled).toBe(false)

    const adoptAction = wrapper.vm.actionDefinitions.find(action => action.api === 'adoptFtctlDrReplica')
    expect(adoptAction.releaseModal).toBe(true)
    expect(adoptAction.disabled).toBe(false)
  })

  it('submits delegated replica-side DR failback with target and replica Mold credentials', async () => {
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'dr',
              drpeersitetype: 'remote-mold',
              remotemoldapiurl: 'http://10.10.22.10:8080/client/api',
              protectionrole: 'standby',
              protectionstate: 'failed_over',
              transportstate: 'failed_over',
              activeside: 'secondary',
              adminstate: 'active',
              fencingstate: 'clear',
              secondaryvirtualmachinestate: 'Running'
            }
          }
        })
      }
      return Promise.resolve({})
    })
    postAPI.mockResolvedValue({
      failbackftctldrreplicaresponse: {
        jobid: 'replica-failback-job-1'
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      failbackFtctlDrReplica: true
    })

    await flushPromises()

    wrapper.vm.openReplicaFailbackModal()
    wrapper.vm.replicaFailbackTargetMoldApiKey = 'source-api-key'
    wrapper.vm.replicaFailbackTargetMoldSecretKey = 'source-secret-key'
    wrapper.vm.replicaFailbackReplicaMoldApiUrl = 'http://10.10.32.10:8080/client/api'
    wrapper.vm.replicaFailbackReplicaMoldApiKey = 'replica-api-key'
    wrapper.vm.replicaFailbackReplicaMoldSecretKey = 'replica-secret-key'

    await wrapper.vm.confirmReplicaFailbackProtection()
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('failbackFtctlDrReplica', {
      virtualmachineid: 'vm-1',
      targetmoldapiurl: 'http://10.10.22.10:8080/client/api',
      targetmoldapikey: 'source-api-key',
      targetmoldsecretkey: 'source-secret-key',
      replicamoldapiurl: 'http://10.10.32.10:8080/client/api',
      replicamoldapikey: 'replica-api-key',
      replicamoldsecretkey: 'replica-secret-key'
    })
    expect(wrapper.vm.$pollJob).toHaveBeenCalledWith(expect.objectContaining({
      jobId: 'replica-failback-job-1',
      title: 'label.ftctl.failback',
      resourceId: 'vm-1'
    }))
    expect(wrapper.vm.showReplicaFailbackModal).toBe(false)
    wrapper.unmount()
  })

  it('shows cloud-managed failed-over primary as stopped without failure summary', async () => {
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: {
              ftctlprotection: {
                enabled: 'true',
                mode: 'ha',
                provisioningbackend: 'cloud-managed',
                protectionstate: 'failed_over',
                transportstate: 'failed_over',
                activeside: 'secondary',
                adminstate: 'active',
                fencingstate: 'manual-fenced'
              }
            }
          })
        case 'getFtctlCheck':
          return Promise.resolve({
            getftctlcheckresponse: {
              ftctlcheck: {
                result: 'ok',
                inventoryresult: 'ok',
                provisioningbackend: 'cloud-managed',
                primaryrc: 1,
                peerrc: 0,
                standbydomainstate: 'running'
              }
            }
          })
        case 'getFtctlHealth':
          return Promise.resolve({
            getftctlhealthresponse: {
              ftctlhealth: {
                result: 'ok',
                rc: 0
              }
            }
          })
        case 'getFtctlEvents':
          return Promise.resolve({
            getftctleventsresponse: {
              ftctlevents: {
                events: [
                  {
                    ts: '2026-05-07T10:00:00+09:00',
                    stage: 'inventory',
                    event: 'inventory.disks',
                    result: 'fail',
                    details: '{"primary_uri":"qemu:///system"}'
                  },
                  { ts: '2026-05-07T10:01:00+09:00', stage: 'failover', event: 'failover.steady', result: 'ok' }
                ]
              }
            }
          })
        default:
          return Promise.resolve({})
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true
    })

    await flushPromises()

    expect(wrapper.vm.primaryExecutionState).toBe('Stopped')
    expect(wrapper.vm.peerExecutionState).toBe('Started')
    expect(wrapper.vm.eventStats.total).toBe(2)
    expect(wrapper.vm.eventStats.fail).toBe(0)
    expect(wrapper.vm.operationalSummary.type).toBe('info')
  })

  it('enables manual fence confirmation for cloud-managed failover-required state', async () => {
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: {
              ftctlprotection: {
                enabled: 'true',
                mode: 'dr',
                provisioningbackend: 'cloud-managed',
                protectionstate: 'failover_required',
                transportstate: 'mirroring',
                activeside: 'primary',
                adminstate: 'active',
                fencingstate: 'required',
                primaryvirtualmachinestate: 'Stopped'
              }
            }
          })
        case 'getFtctlCheck':
          return Promise.resolve({ getftctlcheckresponse: {} })
        case 'getFtctlHealth':
          return Promise.resolve({ getftctlhealthresponse: {} })
        case 'getFtctlEvents':
          return Promise.resolve({ getftctleventsresponse: { events: [] } })
        default:
          return Promise.resolve({})
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      confirmFtctlFence: true
    })

    await flushPromises()

    const confirmAction = wrapper.vm.actionDefinitions.find(action => action.api === 'confirmFtctlFence')
    expect(wrapper.vm.isManualFenceConfirmationReady()).toBe(true)
    expect(confirmAction.disabled).toBe(false)
    expect(confirmAction.reason).toBeNull()
    expect(wrapper.vm.stateTagColor('failover_required')).toBe('orange')
    wrapper.unmount()
  })

  it('submits remote Mold fence confirmation when a refresh race starts after the modal opens', async () => {
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: {
              ftctlprotection: {
                enabled: 'true',
                mode: 'dr',
                drpeersitetype: 'remote-mold',
                remotemoldapiurl: 'http://10.10.32.10:8080/client/api',
                provisioningbackend: 'cloud-managed',
                protectionstate: 'failover_required',
                transportstate: 'mirroring',
                activeside: 'primary',
                adminstate: 'active',
                fencingstate: 'required',
                primaryvirtualmachinestate: 'Stopped'
              }
            }
          })
        case 'getFtctlCheck':
          return Promise.resolve({ getftctlcheckresponse: {} })
        case 'getFtctlHealth':
          return Promise.resolve({ getftctlhealthresponse: {} })
        case 'getFtctlEvents':
          return Promise.resolve({ getftctleventsresponse: { events: [] } })
        default:
          return Promise.resolve({})
      }
    })
    postAPI.mockResolvedValue({
      confirmftctlfenceresponse: {
        jobid: 'remote-fence-job-1'
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      confirmFtctlFence: true
    })

    await flushPromises()

    wrapper.vm.openRemoteFenceModal()
    wrapper.vm.loadingState = true
    wrapper.vm.remoteFenceMoldApiKey = 'remote-api-key'
    wrapper.vm.remoteFenceMoldSecretKey = 'remote-secret-key'

    await wrapper.vm.confirmRemoteFence()
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('confirmFtctlFence', {
      virtualmachineid: 'vm-1',
      remotemoldapiurl: 'http://10.10.32.10:8080/client/api',
      remotemoldapikey: 'remote-api-key',
      remotemoldsecretkey: 'remote-secret-key'
    })
    expect(wrapper.vm.$pollJob).toHaveBeenCalledWith(expect.objectContaining({
      jobId: 'remote-fence-job-1',
      title: 'label.ftctl.confirm.fence',
      resourceId: 'vm-1'
    }))
    expect(wrapper.vm.showRemoteFenceModal).toBe(false)
    expect(wrapper.vm.$message.warning).not.toHaveBeenCalledWith('Another FTCTL refresh is in progress.')
    wrapper.unmount()
  })

  it('runs action, applies payload and emits refresh event', async () => {
    let protectionState = {
      enabled: 'true',
      mode: 'dr',
      protectionstate: 'protected',
      transportstate: 'replicating',
      activeside: 'primary',
      adminstate: 'running',
      fencingstate: 'clear'
    }
    getAPI.mockImplementation((command) => {
      switch (command) {
        case 'getFtctlProtection':
          return Promise.resolve({
            getftctlprotectionresponse: protectionState
          })
        case 'getFtctlCheck':
          return Promise.resolve({ getftctlcheckresponse: {} })
        case 'getFtctlHealth':
          return Promise.resolve({ getftctlhealthresponse: {} })
        case 'getFtctlEvents':
          return Promise.resolve({ getftctleventsresponse: { events: [] } })
        default:
          return Promise.resolve({})
      }
    })
    postAPI.mockResolvedValue({
      failoverftctlprotectionresponse: {
        result: 'ok',
        protectionstate: 'protected',
        transportstate: 'replicating',
        activeside: 'secondary',
        adminstate: 'running',
        fencingstate: 'clear'
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      failoverFtctlProtection: true
    })

    await flushPromises()
    protectionState = {
      enabled: 'true',
      mode: 'dr',
      protectionstate: 'protected',
      transportstate: 'replicating',
      activeside: 'secondary',
      adminstate: 'running',
      fencingstate: 'clear'
    }
    await wrapper.vm.runAction('failoverFtctlProtection')
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('failoverFtctlProtection', { virtualmachineid: 'vm-1' })
    expect(wrapper.vm.protection.activeside).toBe('secondary')
    expect(wrapper.vm.lastAction.success).toBe(true)
    expect(wrapper.vm.lastAction.message).toContain('label.ftctl.failover label.completed')
    expect(eventBus.emit).toHaveBeenCalledWith('vm-refresh-data')
  })

  it('starts async action job polling without waiting for action completion', async () => {
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'ha',
              protectionstate: 'failed_over',
              transportstate: 'failed_over',
              activeside: 'secondary',
              adminstate: 'active',
              fencingstate: 'clear'
            }
          }
        })
      }
      return Promise.resolve({})
    })
    postAPI.mockResolvedValue({
      failbackftctlprotectionresponse: {
        jobid: 'job-1'
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      failbackFtctlProtection: true
    })
    await flushPromises()

    await wrapper.vm.runAction('failbackFtctlProtection')
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('failbackFtctlProtection', { virtualmachineid: 'vm-1' })
    expect(wrapper.vm.$pollJob).toHaveBeenCalledWith(expect.objectContaining({
      jobId: 'job-1',
      title: 'label.ftctl.failback',
      resourceId: 'vm-1'
    }))
    expect(wrapper.vm.actionLoading.failbackFtctlProtection).toBe(false)
    expect(wrapper.vm.lastAction.message).toContain('job-1')
  })

  it('continues DR remote Mold failback with one-time Mold credentials', async () => {
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'dr',
              drpeersitetype: 'remote-mold',
              remotemoldapiurl: 'http://10.10.32.10:8080/client/api',
              protectionstate: 'failing_back',
              transportstate: 'reverse_sync_ready',
              activeside: 'secondary',
              adminstate: 'active',
              fencingstate: 'clear'
            }
          }
        })
      }
      return Promise.resolve({})
    })
    postAPI.mockResolvedValue({
      failbackftctlprotectionresponse: {
        success: true
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      failbackFtctlProtection: true
    })
    await flushPromises()

    expect(wrapper.vm.actionDisabledReason('failbackFtctlProtection')).toBe(null)
    const failbackAction = wrapper.vm.actionDefinitions.find(action => action.api === 'failbackFtctlProtection')
    expect(failbackAction.label).toBe('label.ftctl.continue.failback')
    expect(failbackAction.failbackModal).toBe(true)

    wrapper.vm.openFailbackModal()
    wrapper.vm.failbackMoldApiKey = 'api-key'
    wrapper.vm.failbackMoldSecretKey = 'secret-key'
    await wrapper.vm.confirmFailbackProtection()
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('failbackFtctlProtection', {
      virtualmachineid: 'vm-1',
      failbacktargetmoldtype: 'original-primary',
      remotemoldapiurl: 'http://10.10.32.10:8080/client/api',
      remotemoldapikey: 'api-key',
      remotemoldsecretkey: 'secret-key'
    })
  })

  it('updates sync progress silently without full tab loading', async () => {
    let eventCallCount = 0
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'ha',
              protectionstate: 'syncing',
              transportstate: 'copying',
              activeside: 'primary',
              adminstate: 'active',
              fencingstate: 'clear'
            }
          }
        })
      }
      if (command === 'getFtctlEvents') {
        eventCallCount += 1
        const percent = eventCallCount === 1 ? 10 : 25
        return Promise.resolve({
          getftctleventsresponse: {
            ftctlevents: {
              latestprogress: JSON.stringify({
                direction: 'forward',
                percent,
                copied_bytes: percent,
                total_bytes: 100,
                ready: false
              }),
              events: [{
                timestamp: `2026-05-10T00:00:${String(eventCallCount).padStart(2, '0')}+09:00`,
                event: 'blockcopy.progress',
                details: JSON.stringify({
                  direction: 'forward',
                  percent: 5,
                  copied_bytes: 5,
                  total_bytes: 100,
                  ready: false
                })
              }]
            }
          }
        })
      }
      return Promise.resolve({})
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true
    })
    await flushPromises()
    wrapper.vm.loadingState = false

    await wrapper.vm.fetchSyncProgress()
    await flushPromises()

    expect(wrapper.vm.loadingState).toBe(false)
    expect(wrapper.vm.refreshingProgress).toBe(false)
    expect(wrapper.vm.syncProgressPercent).toBe(25)
    expect(getAPI).toHaveBeenLastCalledWith('getFtctlEvents', { virtualmachineid: 'vm-1', limit: 100 })
    wrapper.unmount()
  })

  it('does not let protection refresh overwrite event progress with stale detail fields', async () => {
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'ha',
              protectionstate: 'syncing',
              transportstate: 'copying',
              activeside: 'primary',
              adminstate: 'active',
              fencingstate: 'clear',
              syncprogresspercent: 4.4,
              syncprogressjson: JSON.stringify({ direction: 'forward', percent: 4.4 })
            }
          }
        })
      }
      if (command === 'getFtctlEvents') {
        return Promise.resolve({
          getftctleventsresponse: {
            ftctlevents: {
              latestprogress: JSON.stringify({
                direction: 'forward',
                percent: 100,
                copied_bytes: 100,
                total_bytes: 100,
                ready: true,
                updated: '2026-05-10T00:01:00+09:00'
              }),
              events: [{
                timestamp: '2026-05-10T00:01:00+09:00',
                event: 'blockcopy.progress',
                details: JSON.stringify({
                  direction: 'forward',
                  percent: 100,
                  copied_bytes: 100,
                  total_bytes: 100,
                  ready: true,
                  updated: '2026-05-10T00:01:00+09:00'
                })
              }]
            }
          }
        })
      }
      return Promise.resolve({})
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true
    })
    await flushPromises()

    expect(wrapper.vm.syncProgressPercent).toBe(100)

    await wrapper.vm.fetchProtection({ silent: true })
    await flushPromises()

    expect(wrapper.vm.syncProgressPercent).toBe(100)
    wrapper.unmount()
  })

  it('keeps pause and resume actions local without parent VM refresh', async () => {
    let protectionCallCount = 0
    getAPI.mockImplementation((command) => {
      if (command === 'getFtctlProtection') {
        protectionCallCount += 1
        return Promise.resolve({
          getftctlprotectionresponse: {
            ftctlprotection: {
              enabled: 'true',
              mode: 'ha',
              protectionstate: 'protected',
              transportstate: 'mirroring',
              activeside: 'primary',
              adminstate: protectionCallCount > 1 ? 'paused' : 'active',
              fencingstate: 'clear'
            }
          }
        })
      }
      return Promise.resolve({})
    })
    postAPI.mockResolvedValue({
      pauseftctlprotectionresponse: {
        result: 'ok',
        protectionstate: 'protected',
        transportstate: 'mirroring',
        activeside: 'primary',
        adminstate: 'paused',
        fencingstate: 'clear'
      }
    })

    const wrapper = createWrapper({
      getFtctlCheck: true,
      getFtctlHealth: true,
      getFtctlEvents: true,
      pauseFtctlProtection: true
    })
    await flushPromises()

    await wrapper.vm.runAction('pauseFtctlProtection')
    await flushPromises()

    expect(postAPI).toHaveBeenCalledWith('pauseFtctlProtection', { virtualmachineid: 'vm-1' })
    expect(wrapper.vm.protection.adminstate).toBe('paused')
    expect(eventBus.emit).not.toHaveBeenCalledWith('vm-refresh-data')
    wrapper.unmount()
  })
})
