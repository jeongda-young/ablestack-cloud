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

<template>
  <a-spin :spinning="fetchLoading">
    <a-list size="small">
      <a-list-item v-if="licenseData.name">
        <div>
          <strong>{{ $t('label.licenseName') }}</strong>
          <div>{{ licenseData.name }}</div>
        </div>
      </a-list-item>
      <a-list-item v-if="licenseData.value">
        <div>
          <strong>{{ $t('label.licenseValue') }}</strong>
          <div>{{ licenseData.value }}</div>
        </div>
      </a-list-item>
      <a-list-item v-if="errorMessage">
        <div>
          <strong>{{ $t('label.error') }}</strong>
          <div>{{ errorMessage }}</div>
        </div>
      </a-list-item>
    </a-list>
  </a-spin>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'LicenseHost',
  data () {
    return {
      fetchLoading: false,
      licenseData: {},
      errorMessage: ''
    }
  },
  created () {
    this.fetchLicenseData()
  },
  methods: {
    fetchLicenseData () {
      this.fetchLoading = true
      api('LicenseHost', { id: this.resource.id })
        .then(response => {
          this.licenseData = response.data
        })
        .catch(error => {
          this.errorMessage = error.message
        })
        .finally(() => {
          this.fetchLoading = false
        })
    }
  }
}
</script>

<style lang="less" scoped>
</style>
