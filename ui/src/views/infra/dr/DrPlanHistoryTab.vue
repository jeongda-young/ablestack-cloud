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
  <div class="cross-dr-history">
    <a-radio-group v-model:value="mode" button-style="solid" size="small" class="cross-dr-history__mode">
      <a-radio-button value="checkpoints">{{ $t('label.dr.sync.history') }}</a-radio-button>
      <a-radio-button value="operations">{{ $t('label.dr.operation.history') }}</a-radio-button>
    </a-radio-group>
    <dr-sync-checkpoints-tab v-if="mode === 'checkpoints'" :planId="planId" />
    <dr-runs-tab v-else :planId="planId" />
  </div>
</template>

<script>
import DrRunsTab from '@/views/infra/dr/DrRunsTab.vue'
import DrSyncCheckpointsTab from '@/views/infra/dr/DrSyncCheckpointsTab.vue'

export default {
  name: 'DrPlanHistoryTab',
  components: { DrRunsTab, DrSyncCheckpointsTab },
  props: {
    planId: { type: String, required: true }
  },
  data () {
    return {
      mode: this.$route.query.history === 'operations' ? 'operations' : 'checkpoints'
    }
  },
  watch: {
    mode (value) {
      this.$router.replace({
        path: this.$route.path,
        query: Object.assign({}, this.$route.query, { history: value })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="less">
.cross-dr-history {
  display: grid;
  gap: 12px;
}

.cross-dr-history__mode {
  justify-self: start;
}
</style>
