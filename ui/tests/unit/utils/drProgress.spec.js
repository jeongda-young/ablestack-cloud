// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.

import { drOperationProgress } from '@/utils/drProgress'

describe('DR operation progress authority', () => {
  it('maps a live failback transfer into the whole operation range', () => {
    expect(drOperationProgress({
      runtype: 'FAILBACK',
      state: 'RUNNING',
      progresspercent: 100,
      transferprogressschemaversion: 2,
      transferbytestotal: 1000,
      transferbytesprocessed: 10,
      transferpercent: 1
    })).toBe(70)
  })

  it('does not show a non-terminal dispatch as complete', () => {
    expect(drOperationProgress({
      runtype: 'FAILBACK',
      state: 'DISPATCHING',
      progresspercent: 100
    })).toBe(15)
  })

  it('shows one hundred only for a terminal run', () => {
    expect(drOperationProgress({ state: 'SUCCEEDED', progresspercent: 100 })).toBe(100)
  })
})
