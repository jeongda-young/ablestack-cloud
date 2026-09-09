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
  <div :class="['cross-dr-kpi', breached ? 'cross-dr-kpi--warning' : '']">
    <div class="cross-dr-kpi__label">{{ label }}</div>
    <div class="cross-dr-kpi__value">{{ formattedValue }}</div>
    <div v-if="targetText" class="cross-dr-kpi__meta">{{ targetText }}</div>
    <div v-if="asOfText" class="cross-dr-kpi__meta">{{ asOfText }}</div>
  </div>
</template>

<script>
export default {
  name: 'DrRpoKpi',
  props: {
    label: {
      type: String,
      required: true
    },
    seconds: {
      type: [Number, String],
      default: null
    },
    targetSeconds: {
      type: [Number, String],
      default: null
    },
    evaluationMode: {
      type: String,
      default: 'LIVE'
    },
    asOf: {
      type: [String, Date],
      default: null
    },
    status: {
      type: String,
      default: 'UNKNOWN'
    }
  },
  computed: {
    numericSeconds () {
      const value = Number(this.seconds)
      return Number.isFinite(value) ? value : null
    },
    numericTargetSeconds () {
      const value = Number(this.targetSeconds)
      return Number.isFinite(value) ? value : null
    },
    breached () {
      const status = String(this.status || '').toUpperCase()
      if (status === 'MISSED') {
        return true
      }
      if (status === 'MET') {
        return false
      }
      if (!['LIVE', 'REVERSE_LIVE'].includes(String(this.evaluationMode || '').toUpperCase())) {
        return false
      }
      return this.numericSeconds !== null && this.numericTargetSeconds !== null && this.numericSeconds > this.numericTargetSeconds
    },
    formattedValue () {
      return this.formatSeconds(this.numericSeconds)
    },
    targetText () {
      if (this.numericTargetSeconds === null) {
        return ''
      }
      return this.$t('label.dr.target') + ': ' + this.formatSeconds(this.numericTargetSeconds)
    },
    asOfText () {
      if (!this.asOf) {
        return ''
      }
      return this.$t('label.dr.rpo.as.of') + ': ' + this.asOf
    }
  },
  methods: {
    formatSeconds (seconds) {
      if (seconds === null || seconds === undefined || seconds < 0) {
        return '-'
      }
      if (seconds < 60) {
        return `${Math.round(seconds)}s`
      }
      if (seconds < 3600) {
        return `${Math.round(seconds / 60)}m`
      }
      if (seconds < 86400) {
        return `${Math.round(seconds / 3600)}h`
      }
      return `${Math.round(seconds / 86400)}d`
    }
  }
}
</script>

<style lang="less">
.cross-dr-kpi {
  min-width: 140px;
  padding: 10px 12px;
  border: 1px solid var(--cross-dr-border, #e8e8e8);
  border-radius: 6px;
  background: var(--cross-dr-surface-muted, #fafafa);
}

.cross-dr-kpi__label {
  color: var(--cross-dr-text-secondary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
  line-height: 18px;
}

.cross-dr-kpi__value {
  color: var(--cross-dr-text, rgba(0, 0, 0, 0.85));
  font-size: 22px;
  font-weight: 600;
  line-height: 30px;
}

.cross-dr-kpi__meta {
  color: var(--cross-dr-text-secondary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
  line-height: 18px;
}

.cross-dr-kpi--warning {
  border-color: rgba(250, 173, 20, 0.7);
  background: #fffbe6;
}

body.dark-mode .cross-dr-kpi {
  --cross-dr-border: rgba(255, 255, 255, 0.12);
  --cross-dr-surface-muted: rgba(255, 255, 255, 0.06);
  --cross-dr-text: rgba(255, 255, 255, 0.86);
  --cross-dr-text-secondary: rgba(255, 255, 255, 0.58);
}

body.dark-mode .cross-dr-kpi--warning {
  border-color: rgba(250, 173, 20, 0.44);
  background: rgba(250, 173, 20, 0.12);
}
</style>
