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
    <a-empty v-if="events.length === 0" :description="$t('message.dr.no.events')" />
    <div v-else class="cross-dr-event-log">
      <div v-for="event in events" :key="event.id" class="cross-dr-event-log__item">
        <div class="cross-dr-event-log__head">
          <div>
            <div class="cross-dr-event-log__title">{{ event.message || event.eventtype || '-' }}</div>
            <div class="cross-dr-event-log__meta">
              {{ event.created || '-' }} | {{ event.source || '-' }}
              <span v-if="event.runid"> | {{ $t('label.dr.run') }}: {{ event.runid }}</span>
            </div>
          </div>
          <dr-status-pill :status="event.severity || 'INFO'" />
        </div>
        <a-collapse v-if="event.details" ghost class="cross-dr-event-log__details">
          <a-collapse-panel key="details" :header="$t('label.details')">
            <pre class="cross-dr-code">{{ event.details }}</pre>
          </a-collapse-panel>
        </a-collapse>
      </div>
    </div>
  </a-spin>
</template>

<script>
import DrStatusPill from '@/components/dr/DrStatusPill.vue'
import { listDrEvents } from '@/api/dr'

export default {
  name: 'DrEventsTab',
  components: {
    DrStatusPill
  },
  props: {
    planId: {
      type: String,
      default: ''
    },
    runId: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      loading: false,
      events: []
    }
  },
  watch: {
    planId () {
      this.fetchData()
    },
    runId () {
      this.fetchData()
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      if (!('listDrEvents' in this.$store.getters.apis) || (!this.planId && !this.runId)) {
        this.events = []
        return
      }
      const params = this.runId ? { runid: this.runId, page: 1, pagesize: 20 } : { planid: this.planId, page: 1, pagesize: 20 }
      this.loading = true
      listDrEvents(params).then(result => {
        this.events = (result.items || []).slice(0, 20)
      }).catch(error => {
        this.events = []
        this.$notification.error({
          message: this.$t('label.events'),
          description: error?.response?.data?.errorresponse?.errortext || error?.message || this.$t('label.error')
        })
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style lang="less">
.cross-dr-event-log {
  display: grid;
  gap: 10px;
}

.cross-dr-event-log__item {
  padding: 12px;
  border: 1px solid var(--cross-dr-border, #e8e8e8);
  border-radius: 6px;
  background: var(--cross-dr-surface, #ffffff);
}

.cross-dr-event-log__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.cross-dr-event-log__title {
  color: var(--cross-dr-text, rgba(0, 0, 0, 0.85));
  font-weight: 600;
  line-height: 22px;
  overflow-wrap: anywhere;
}

.cross-dr-event-log__meta {
  color: var(--cross-dr-text-secondary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
  line-height: 18px;
}

.cross-dr-event-log__details {
  margin-top: 8px;
}

.cross-dr-code {
  max-height: 260px;
  margin: 0;
  padding: 10px;
  overflow: auto;
  border-radius: 6px;
  background: var(--cross-dr-code-bg, #f6f8fa);
  color: var(--cross-dr-text, rgba(0, 0, 0, 0.85));
  white-space: pre-wrap;
  word-break: break-word;
}

body.dark-mode .cross-dr-event-log__item,
body.dark-mode .cross-dr-code {
  --cross-dr-border: rgba(255, 255, 255, 0.12);
  --cross-dr-surface: rgba(255, 255, 255, 0.04);
  --cross-dr-code-bg: rgba(0, 0, 0, 0.24);
  --cross-dr-text: rgba(255, 255, 255, 0.86);
  --cross-dr-text-secondary: rgba(255, 255, 255, 0.58);
}
</style>
