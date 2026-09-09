// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.

import { shallowMount } from '@vue/test-utils'
import DrRpoKpi from '@/components/dr/DrRpoKpi.vue'

const mountKpi = props => shallowMount(DrRpoKpi, {
  props: {
    label: 'RPO',
    ...props
  },
  global: {
    mocks: {
      $t: key => key
    }
  }
})

describe('DrRpoKpi authority-aware presentation', () => {
  it('uses API status for a frozen cutover RPO', () => {
    const wrapper = mountKpi({
      seconds: 36,
      targetSeconds: 300,
      evaluationMode: 'CUTOVER_FROZEN',
      status: 'MET',
      asOf: '2026-07-30T11:06:33+09:00'
    })

    expect(wrapper.vm.breached).toBe(false)
    expect(wrapper.vm.asOfText).toContain('2026-07-30T11:06:33+09:00')
  })

  it('marks a live RPO over target as breached', () => {
    const wrapper = mountKpi({
      seconds: 301,
      targetSeconds: 300,
      evaluationMode: 'LIVE',
      status: 'UNKNOWN'
    })

    expect(wrapper.vm.breached).toBe(true)
  })
})
