<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<template>
  <a-spin :spinning="loading">
    <div class="cross-dr-tab-toolbar">
      <a-button size="small" @click="fetchData">
        <template #icon><ReloadOutlined /></template>
        {{ $t('label.refresh') }}
      </a-button>
    </div>
    <a-table
      size="small"
      :columns="columns"
      :dataSource="runs"
      :rowKey="record => record.id"
      :pagination="{ pageSize: 10 }"
      :expandRowByClick="true">
      <template #expandedRowRender="{ record }">
        <dr-run-progress :run="record" :steps="record.steps || []" />
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.key === 'state'">
          <dr-status-pill :status="text" />
        </template>
        <template v-else-if="column.key === 'progresspercent'">
          <span v-if="progressValue(record) !== null">{{ progressValue(record) }}%</span>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'id'">
          <router-link :to="{ path: '/drplan/' + planId, query: { tab: 'history', history: 'operations', runid: record.id } }">{{ text }}</router-link>
        </template>
      </template>
    </a-table>
  </a-spin>
</template>

<script>
import DrRunProgress from '@/components/dr/DrRunProgress.vue'
import DrStatusPill from '@/components/dr/DrStatusPill.vue'
import { listDrRuns } from '@/api/dr'
import { drOperationProgress } from '@/utils/drProgress'

export default {
  name: 'DrRunsTab',
  components: {
    DrRunProgress,
    DrStatusPill
  },
  props: {
    planId: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      runs: [],
      pollTimer: null,
      pollInFlight: false,
      pollIntervalMs: 5000,
      columns: [
        { key: 'id', title: this.$t('label.id'), dataIndex: 'id' },
        { key: 'runtype', title: this.$t('label.dr.run.type'), dataIndex: 'runtype' },
        { key: 'state', title: this.$t('label.state'), dataIndex: 'state' },
        { key: 'currentstep', title: this.$t('label.dr.current.step'), dataIndex: 'currentstep' },
        { key: 'progresspercent', title: this.$t('label.progress'), dataIndex: 'progresspercent' },
        { key: 'created', title: this.$t('label.created'), dataIndex: 'created' },
        { key: 'completed', title: this.$t('label.completed'), dataIndex: 'completed' },
        { key: 'errorcode', title: this.$t('label.error.code'), dataIndex: 'errorcode' }
      ]
    }
  },
  watch: {
    planId () {
      this.stopPolling()
      this.fetchData()
    }
  },
  created () {
    this.fetchData()
  },
  beforeUnmount () {
    this.stopPolling()
  },
  methods: {
    fetchData (options = {}) {
      if (!this.planId || !('listDrRuns' in this.$store.getters.apis)) {
        this.runs = []
        this.stopPolling()
        return Promise.resolve()
      }
      const silent = options.silent === true
      if (!silent) {
        this.loading = true
      }
      return listDrRuns({ planid: this.planId }).then(result => {
        this.runs = result.items || []
      }).finally(() => {
        if (!silent) {
          this.loading = false
        }
        this.schedulePolling()
      })
    },
    isActiveRun (run) {
      return ['QUEUED', 'DISPATCHING', 'ACCEPTED', 'RUNNING', 'CANCEL_REQUESTED'].includes(String(run?.state || '').toUpperCase())
    },
    progressValue (run) {
      const progress = drOperationProgress(run)
      return progress > 0 ? progress : null
    },
    hasActiveRun () {
      return this.runs.some(run => this.isActiveRun(run))
    },
    schedulePolling () {
      if (!this.hasActiveRun()) {
        this.stopPolling()
        return
      }
      if (this.pollTimer) {
        return
      }
      this.pollTimer = window.setInterval(this.pollRuns, this.pollIntervalMs)
    },
    stopPolling () {
      if (this.pollTimer) {
        window.clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    pollRuns () {
      if (this.pollInFlight || !this.planId) {
        return
      }
      this.pollInFlight = true
      this.fetchData({ silent: true }).finally(() => {
        this.pollInFlight = false
      })
    }
  }
}
</script>

<style lang="less">
.cross-dr-tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
</style>
