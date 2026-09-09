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
  <div class="cross-dr-action-menu-shell">
    <div v-if="showResourceTitle && resolvedTitle" class="cross-dr-action-menu__title">
      {{ resolvedTitle }}
    </div>
    <a-menu class="cross-dr-action-menu" :selectable="false">
      <a-menu-item-group v-for="group in groupedActions" :key="group.key">
        <template #title>{{ $t(group.label) }}</template>
        <a-menu-item
          v-for="entry in group.actions"
          :key="entry.action.key || entry.action.api"
          :disabled="!entry.state.enabled"
          :class="{
            'cross-dr-action-menu__item--danger': entry.action.danger && entry.state.enabled
          }"
          @click="execute(entry)">
          <a-tooltip :title="disabledReason(entry)">
            <span class="cross-dr-action-menu__item-content">
              <render-icon :icon="entry.action.icon" />
              <span>{{ $t(entry.action.label) }}</span>
            </span>
          </a-tooltip>
        </a-menu-item>
      </a-menu-item-group>
    </a-menu>
  </div>
</template>

<script>
import RenderIcon from '@/utils/renderIcon'
import { drActionReasonMessageKey, resolveDrActionAvailability } from '@/utils/dr/actionAvailability'

const GROUPS = [
  { key: 'MULTI', label: 'label.dr.action.group.multiple' },
  { key: 'CURRENT', label: 'label.dr.action.group.current' },
  { key: 'PLAN', label: 'label.dr.action.group.plan' },
  { key: 'REPLICATION', label: 'label.dr.action.group.replication' },
  { key: 'TEST', label: 'label.dr.action.group.test' },
  { key: 'TRANSITION', label: 'label.dr.action.group.transition' },
  { key: 'ADVANCED', label: 'label.dr.action.group.advanced' },
  { key: 'PROTECTION_END', label: 'label.dr.action.group.protection.end' },
  { key: 'GENERAL', label: 'label.actions' }
]

export default {
  name: 'DrActionMenuContent',
  components: {
    RenderIcon
  },
  props: {
    actions: {
      type: Array,
      default: () => []
    },
    resource: {
      type: Object,
      default: () => ({})
    },
    title: {
      type: String,
      default: ''
    },
    showResourceTitle: {
      type: Boolean,
      default: false
    }
  },
  emits: ['exec-action'],
  computed: {
    resolvedTitle () {
      return this.title || this.resource?.name || this.resource?.displayname || ''
    },
    resolvedActions () {
      return this.actions
        .map(action => {
          const currentRun = action.currentRun || {}
          const state = resolveDrActionAvailability(action, this.resource, currentRun)
          if (state.applicable && action.api && !(action.api in this.$store.getters.apis)) {
            state.enabled = false
            state.reasonCode = 'DR_ACTION_API_UNAVAILABLE'
          }
          return { action, state }
        })
        .filter(entry => entry.state.applicable)
    },
    groupedActions () {
      return GROUPS
        .map(group => ({
          key: group.key,
          label: group.label,
          actions: this.resolvedActions.filter(entry => (entry.action.group || 'GENERAL') === group.key)
        }))
        .filter(group => group.actions.length > 0)
    }
  },
  methods: {
    disabledReason (entry) {
      if (entry.state.enabled) {
        return ''
      }
      const key = drActionReasonMessageKey(entry.state.reasonCode)
      return this.$te && this.$te(key)
        ? this.$t(key, entry.state.reasonArgs || {})
        : this.$t('message.dr.action.not.eligible')
    },
    execute (entry) {
      if (!entry.state.enabled) {
        return
      }
      this.$emit('exec-action', Object.assign({}, entry.action, { resource: this.resource }))
    }
  }
}
</script>
