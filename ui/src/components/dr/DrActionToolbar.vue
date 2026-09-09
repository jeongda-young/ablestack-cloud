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
  <a-space
    :class="['cross-dr-action-toolbar', {
      'cross-dr-action-toolbar--compact': compact,
      'cross-dr-action-toolbar--dataview': dataView
    }]"
    wrap>
    <template v-for="action in visibleActions" :key="action.key">
      <a-tooltip :title="disabledReason(action)">
        <a-popconfirm
          v-if="action.danger && !action.modal"
          :title="$t(action.confirmMessage)"
          :ok-text="$t('label.yes')"
          :cancel-text="$t('label.no')"
          :disabled="isDisabled(action)"
          @confirm="run(action)">
          <a-button
            size="small"
            :type="dataView ? 'text' : 'default'"
            :shape="dataView ? null : 'circle'"
            :class="{ 'action-button-item': dataView, 'action-button-item--dataview': dataView }"
            :danger="action.danger"
            :disabled="isDisabled(action)"
            :loading="loadingAction === action.command">
            <template #icon><component :is="action.icon" /></template>
            <span v-if="!compact || dataView">{{ $t(action.label) }}</span>
          </a-button>
        </a-popconfirm>
        <a-button
          v-else
          size="small"
          :type="dataView ? 'text' : 'default'"
          :shape="dataView ? null : 'circle'"
          :class="{ 'action-button-item': dataView, 'action-button-item--dataview': dataView }"
          :danger="action.danger"
          :disabled="isDisabled(action)"
          :loading="loadingAction === action.command"
          @click="run(action)">
          <template #icon><component :is="action.icon" /></template>
          <span v-if="!compact || dataView">{{ $t(action.label) }}</span>
        </a-button>
      </a-tooltip>
    </template>
  </a-space>
</template>

<script>
import { buildDrPlanActions } from '@/utils/dr/resourceActions'
import { drActionReasonMessageKey, resolveDrActionAvailability } from '@/utils/dr/actionAvailability'

export default {
  name: 'DrActionToolbar',
  props: {
    plan: {
      type: Object,
      required: true
    },
    loadingAction: {
      type: String,
      default: ''
    },
    compact: {
      type: Boolean,
      default: false
    },
    dataView: {
      type: Boolean,
      default: false
    },
    currentRun: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['run-action'],
  computed: {
    actions () {
      return buildDrPlanActions(this.currentRun).filter(action => action.command)
    },
    visibleActions () {
      return this.actions.filter(action => this.isVisible(action))
    }
  },
  methods: {
    isVisible (action) {
      return resolveDrActionAvailability(action, this.plan, this.currentRun).applicable
    },
    isDisabled (action) {
      const state = resolveDrActionAvailability(action, this.plan, this.currentRun)
      return !(action.api in this.$store.getters.apis) || !state.enabled
    },
    disabledReason (action) {
      if (!(action.api in this.$store.getters.apis)) {
        return this.$t('message.dr.action.api.unavailable')
      }
      const state = resolveDrActionAvailability(action, this.plan, this.currentRun)
      if (!state.enabled) {
        const key = drActionReasonMessageKey(state.reasonCode)
        return this.$te && this.$te(key)
          ? this.$t(key, state.reasonArgs || {})
          : this.$t('message.dr.action.not.eligible')
      }
      return ''
    },
    run (action) {
      if (this.isDisabled(action)) {
        return
      }
      this.$emit('run-action', Object.assign({}, action, { currentRun: this.currentRun || {} }))
    }
  }
}
</script>

<style lang="less">
.cross-dr-action-toolbar {
  max-width: 100%;
}

.cross-dr-action-toolbar--dataview {
  display: flex;
  flex-direction: column;
  width: max-content;
  min-width: 220px;
  max-width: 300px;
}

.cross-dr-action-toolbar--dataview .ant-space-item {
  width: 100%;
}

.cross-dr-action-toolbar--dataview .ant-btn {
  justify-content: flex-start;
  width: 100%;
  margin-left: 0 !important;
}

.cross-dr-action-toolbar--dataview .ant-btn span + span {
  margin-left: 8px;
}

.cross-dr-action-toolbar .ant-btn:not(.ant-btn-circle) {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
