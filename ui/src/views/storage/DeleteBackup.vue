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
  <div class="backup-delete-dialog">
    <p>{{ $t('message.delete.backup') }}</p>
    <a-form layout="vertical">
      <a-form-item :label="$t('label.forced')" :extra="$t(hasAblestack ? 'message.backup.delete.force' : 'message.vmbackup.force')">
        <a-switch v-model:checked="forced" :disabled="busy" />
      </a-form-item>
    </a-form>
    <a-spin :spinning="loading">
      <a-alert v-if="error" type="error" show-icon :message="error" />
      <a-alert v-if="hasAblestack" :type="forced ? 'warning' : 'info'" show-icon :message="$t(forced ? 'message.backup.delete.force' : 'message.backup.delete.normal')" />
      <a-alert v-if="hasOther" type="warning" show-icon :message="$t('message.vmbackup.delete')" />
      <div v-if="hasAblestack && !loading && !error" class="backup-delete-scope">
        <strong>{{ $t('label.backup.delete.affected') }} ({{ affected.length }})</strong>
        <ul><li v-for="point in affected" :key="point.id">{{ point.name || point.id }} · {{ point.type }}</li></ul>
        <template v-if="deferred.length">
          <p>{{ $t('message.backup.delete.deferred') }}</p>
          <ul><li v-for="point in deferred" :key="point.id">{{ point.name || point.id }} · {{ point.type }}</li></ul>
        </template>
      </div>
    </a-spin>
    <div class="backup-delete-footer">
      <a-button :disabled="busy" @click="$emit('close-action')">{{ $t('label.cancel') }}</a-button>
      <a-button v-if="error" :disabled="busy || loading" @click="loadPlans">{{ $t('label.refresh') }}</a-button>
      <a-button type="primary" danger :loading="busy" :disabled="loading || !!error || !records.length" @click="submit">{{ $t('label.delete.backup') }}</a-button>
    </div>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import { isAblestackBackupProvider } from '@/utils/ablestackBackupPolicy'

export default {
  name: 'DeleteBackup',
  props: {
    resource: { type: Object, default: () => ({}) },
    currentAction: { type: Object, default: () => ({}) },
    selectedItems: { type: Array, default: () => [] },
    selectedRowKeys: { type: Array, default: () => [] }
  },
  emits: ['close-action', 'refresh-data', 'refresh'],
  data () { return { records: [], plans: [], forced: false, loading: false, busy: false, error: '', revision: 0 } },
  computed: {
    hasAblestack () { return this.records.some(isAblestackBackupProvider) },
    hasOther () { return this.records.some(record => !isAblestackBackupProvider(record)) },
    affected () { return this.unique(this.plans.flatMap(plan => plan.backup || [])) },
    deferred () { return this.unique(this.plans.filter(plan => plan.state === 'WAITING_DEPENDENCIES').flatMap(plan => plan.dependentbackup || [])) }
  },
  watch: {
    forced () { if (this.records.length) this.loadPlans() },
    resource: { handler () { this.initialize() }, deep: true }
  },
  mounted () { this.initialize() },
  activated () { this.initialize() },
  beforeUnmount () { this.revision++ },
  methods: {
    unique (points) { return [...new Map(points.map(point => [point.id, point])).values()] },
    async initialize () {
      if (this.busy) return
      this.forced = this.resource.deletionforced === true
      this.records = []
      const ids = this.currentAction.invokedAsGroupAction ? this.selectedRowKeys : [this.resource.id]
      const token = ++this.revision
      this.loading = true; this.error = ''; this.plans = []
      try {
        const records = await Promise.all(ids.map(async id => {
          const response = await getAPI('listBackups', { id, listall: true })
          const record = response.listbackupsresponse?.backup?.find(point => point.id === id)
          if (!record) throw new Error(this.$t('message.vmbackup.unavailable'))
          return record
        }))
        if (token !== this.revision) return
        this.records = records
        await this.loadPlans()
      } catch (error) { if (token === this.revision) { this.error = error.message || String(error); this.loading = false } }
    },
    async loadPlans () {
      const token = ++this.revision
      const forced = this.forced
      this.loading = true; this.error = ''; this.plans = []
      try {
        const plans = await Promise.all(this.records.filter(isAblestackBackupProvider).map(async record => {
          const response = await getAPI('getBackupDeletionPlan', { id: record.id, forced })
          const plan = response.getbackupdeletionplanresponse?.backupdeletionplan
          if (!plan?.deletionplan || !plan.backup?.length) throw new Error(this.$t('message.backup.delete.preview.failed'))
          return plan
        }))
        if (token === this.revision) this.plans = plans
      } catch (error) { if (token === this.revision) this.error = error.message || String(error) } finally { if (token === this.revision) this.loading = false }
    },
    async submit () {
      if (this.busy || this.loading || this.error) return
      this.busy = true
      const forced = this.forced
      // An ancestor's forced scope already contains a selected descendant. Submit it once.
      const roots = this.plans.filter(plan => !forced || !this.plans.some(other => other.id !== plan.id && other.backup.some(point => point.id === plan.id)))
      const planned = this.records.filter(record => !isAblestackBackupProvider(record)).map(record => ({ id: record.id }))
      planned.push(...roots)
      try {
        for (const plan of planned) {
          const response = await postAPI('deleteBackup', { id: plan.id, forced, ...(plan.deletionplan ? { deletionplan: plan.deletionplan } : {}) })
          const jobId = response.deletebackupresponse?.jobid
          if (!jobId) throw new Error(this.$t('message.backup.delete.unconfirmed'))
          // Keep bulk operations ordered; a failed or unconfirmed deletion must stop the batch.
          await new Promise((resolve, reject) => {
            this.$pollJob({
              jobId,
              originalPage: this.$route.path,
              title: this.$t('label.delete.backup'),
              description: plan.id,
              showLoading: false,
              showSuccessMessage: false,
              successMethod: resolve,
              errorMethod: reject,
              catchMethod: reject
            }).catch(reject)
          })
        }
        this.$emit('refresh-data'); this.$emit('refresh'); this.$emit('close-action')
      } catch (error) {
        this.error = error?.jobresult?.errortext || error?.message || this.$t('message.backup.delete.unconfirmed')
        this.$emit('refresh-data'); this.$emit('refresh')
      } finally { this.busy = false }
    }
  }
}
</script>

<style scoped>
.backup-delete-dialog { min-width: 420px; max-width: 800px; }
.backup-delete-scope { margin-top: 16px; max-height: 350px; overflow-y: auto; overflow-wrap: anywhere; }
.backup-delete-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
</style>
