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

import { shallowMount } from '@vue/test-utils'
import DrPlanOverview from '@/views/infra/dr/DrPlanOverview.vue'

const translations = {
  'message.dr.error.dr.guest.os.unsupported': 'Guest operating system could not be identified.',
  'message.dr.reprotect.required': 'Run reprotect.',
  'label.dr.rpo.at.failover': 'RPO at failover'
}

const createWrapper = (plan, currentRun = {}) => {
  return shallowMount(DrPlanOverview, {
    props: {
      plan,
      currentRun,
      showDetails: false,
      showProtectionSummary: true
    },
    global: {
      mocks: {
        $store: {
          state: {
            app: {
              device: 'desktop'
            }
          }
        },
        $t: key => translations[key] || key,
        $te: key => Object.prototype.hasOwnProperty.call(translations, key)
      }
    }
  })
}

describe('DrPlanOverview current warning projection', () => {
  test('does not display a historical runtime error when protection is ready', () => {
    const wrapper = createWrapper({
      state: 'READY',
      effectivestate: 'READY',
      protectionstate: 'READY',
      runtimeerrorcode: 'DR_GUEST_OS_UNSUPPORTED',
      lasterrormessage: 'Historical test failover failed'
    })

    expect(wrapper.vm.hasCurrentRisk).toBe(false)
    expect(wrapper.vm.visibleErrorCode).toBe('')
    expect(wrapper.find('.cross-dr-risk').exists()).toBe(false)
  })

  test('displays a translated current protection error', () => {
    const wrapper = createWrapper({
      state: 'ERROR',
      effectivestate: 'ERROR',
      protectionstate: 'ERROR',
      runtimeerrorcode: 'DR_GUEST_OS_UNSUPPORTED'
    })

    expect(wrapper.vm.hasCurrentRisk).toBe(true)
    expect(wrapper.vm.visibleErrorCode).toBe('DR_GUEST_OS_UNSUPPORTED')
    expect(wrapper.vm.translatedVisibleError).toBe('Guest operating system could not be identified.')
  })

  test('displays projection integrity failures without using run history', () => {
    const wrapper = createWrapper({
      state: 'READY',
      effectivestate: 'READY',
      protectionstate: 'READY',
      projectionintegritystate: 'INCONSISTENT',
      projectionintegritycode: 'DR_PROJECTION_INCONSISTENT'
    })

    expect(wrapper.vm.hasCurrentRisk).toBe(true)
    expect(wrapper.vm.visibleErrorCode).toBe('DR_PROJECTION_INCONSISTENT')
  })

  test('shows reprotect guidance without a generic error after failover', () => {
    const wrapper = createWrapper({
      state: 'FAILED_OVER',
      currentseverity: 'INFO',
      protectionstate: 'FAILED_OVER_UNPROTECTED',
      protectionphase: 'FAILED_OVER_UNPROTECTED',
      rpoevaluationmode: 'CUTOVER_FROZEN',
      displayrposeconds: 36
    })

    expect(wrapper.vm.currentProtectionFailed).toBe(false)
    expect(wrapper.vm.reprotectRequired).toBe(true)
    expect(wrapper.vm.riskAlertType).toBe('info')
    expect(wrapper.vm.riskSummary).toBe('Run reprotect.')
  })

  test('does not expose transient source worker placement in plan details', () => {
    const wrapper = createWrapper({
      state: 'NEW',
      sourceworkerhostuuid: 'source-host-uuid',
      sourceworkerhostname: 'ablecube13-1'
    })

    expect(wrapper.vm.sourceWorkerHostLabel).toBeUndefined()
    expect(wrapper.vm.detailFields.find(field => field.key === 'sourceWorkerHost')).toBeUndefined()
  })

  test('does not expose persisted source mapping as execution authority', () => {
    const wrapper = createWrapper({
      state: 'NEW',
      mappingjson: JSON.stringify({
        source: {
          hardware: {
            sourceHostUuid: 'source-host-uuid',
            sourceHostName: 'ablecube13-1'
          }
        }
      })
    })

    expect(wrapper.vm.sourceWorkerHostLabel).toBeUndefined()
    expect(wrapper.vm.detailFields.find(field => field.key === 'sourceWorkerHost')).toBeUndefined()
  })
})
