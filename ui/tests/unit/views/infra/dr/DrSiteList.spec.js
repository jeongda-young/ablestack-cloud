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

import DrSiteList from '@/views/infra/dr/DrSiteList.vue'
import { createI18n } from 'vue-i18n'
import en from '@public/locales/en.json'
import koKR from '@public/locales/ko_KR.json'

describe('DrSiteList create form contract', () => {
  test('offers only ABLESTACK and VMware Direct for new sites', () => {
    expect(DrSiteList.computed.siteTypeOptions()).toEqual([
      { value: 'MOLD_KVM', label: 'label.dr.site.type.mold.kvm' },
      { value: 'VMWARE_DIRECT', label: 'label.dr.site.type.vmware.direct' }
    ])
  })

  test('keeps an explicit VMware Direct username control in the render output', () => {
    const renderOutput = DrSiteList.render.toString()
    expect(renderOutput).toContain('vcenter-username')
    expect(renderOutput).toContain('dr-site-vcenter-username')
  })

  test.each([
    ['en', en],
    ['ko_KR', koKR]
  ])('compiles the vCenter username placeholder for %s', (locale, messages) => {
    const i18n = createI18n({
      locale,
      messages: { [locale]: messages }
    })

    expect(i18n.global.t('message.dr.site.vcenter.username.placeholder')).toBe('administrator@vsphere.local')
  })
})
