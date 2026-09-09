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
  <span :class="['cross-dr-status-pill', statusClass]">
    <span class="cross-dr-status-pill__dot"></span>
    <span class="cross-dr-status-pill__label">{{ displayText }}</span>
  </span>
</template>

<script>
const successStates = ['READY', 'CONNECTED', 'CONFIGURED', 'TARGET_READY', 'TARGET_PROTECTED', 'SUCCEEDED', 'ENABLED', 'ACTIVE']
const warningStates = ['DEGRADED', 'RPO_DUE_SOON', 'RPO_EXCEEDED', 'STALE', 'PAUSED', 'DISABLED', 'UNPROTECTED', 'FAILBACK_READY', 'FAILED_OVER_UNPROTECTED', 'RETRYING', 'WAITING_SOURCE_RECOVERY', 'DR_PROJECTION_STALE', 'DR_ENGINE_BUSY_RETRYABLE', 'COMMIT_VERIFYING', 'COMMIT_UNCERTAIN', 'ROLLBACK_FENCING', 'SKIPPED', 'CONSISTENCY_WARNING']
const errorStates = ['ERROR', 'FAILED', 'OVERDUE', 'DISCONNECTED', 'FENCED', 'DR_ENGINE_WORKER_STALLED', 'DR_ENGINE_WORKER_FAILED', 'BLOCKED']
const infoStates = ['SYNCING', 'MATERIALIZING', 'RUNNING', 'QUEUED', 'CREATED', 'TESTING', 'REPROTECTING', 'PREPARING', 'DISPATCHING', 'ACCEPTED', 'RESULT_FINALIZING', 'RECOVERING_BASELINE']

export default {
  name: 'DrStatusPill',
  props: {
    status: {
      type: [String, Number, Boolean],
      default: ''
    }
  },
  computed: {
    normalizedStatus () {
      return String(this.status || 'UNKNOWN').toUpperCase()
    },
    displayText () {
      const key = `label.dr.state.${this.normalizedStatus.toLowerCase()}`
      if (typeof this.$te === 'function' && this.$te(key)) {
        return this.$t(key)
      }
      return this.normalizedStatus.replace(/_/g, ' ')
    },
    statusClass () {
      if (successStates.includes(this.normalizedStatus)) {
        return 'cross-dr-status-pill--success'
      }
      if (warningStates.includes(this.normalizedStatus)) {
        return 'cross-dr-status-pill--warning'
      }
      if (errorStates.includes(this.normalizedStatus)) {
        return 'cross-dr-status-pill--error'
      }
      if (infoStates.includes(this.normalizedStatus)) {
        return 'cross-dr-status-pill--info'
      }
      return 'cross-dr-status-pill--neutral'
    }
  }
}
</script>

<style lang="less">
.cross-dr-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  padding: 2px 9px;
  border: 1px solid var(--cross-dr-status-border, rgba(0, 0, 0, 0.08));
  border-radius: 999px;
  background: var(--cross-dr-status-bg, #f5f5f5);
  color: var(--cross-dr-status-text, rgba(0, 0, 0, 0.72));
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
}

.cross-dr-status-pill__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--cross-dr-status-dot, #8c8c8c);
}

.cross-dr-status-pill--success {
  --cross-dr-status-bg: #f0f9f0;
  --cross-dr-status-border: #b7eb8f;
  --cross-dr-status-dot: #389e0d;
  --cross-dr-status-text: #237804;
}

.cross-dr-status-pill--warning {
  --cross-dr-status-bg: #fff8e6;
  --cross-dr-status-border: #ffe58f;
  --cross-dr-status-dot: #d48806;
  --cross-dr-status-text: #874d00;
}

.cross-dr-status-pill--error {
  --cross-dr-status-bg: #fff1f0;
  --cross-dr-status-border: #ffa39e;
  --cross-dr-status-dot: #cf1322;
  --cross-dr-status-text: #a8071a;
}

.cross-dr-status-pill--info {
  --cross-dr-status-bg: #e6f7ff;
  --cross-dr-status-border: #91d5ff;
  --cross-dr-status-dot: #096dd9;
  --cross-dr-status-text: #0050b3;
}

.cross-dr-status-pill--neutral {
  --cross-dr-status-bg: #f5f5f5;
  --cross-dr-status-border: #d9d9d9;
  --cross-dr-status-dot: #8c8c8c;
  --cross-dr-status-text: rgba(0, 0, 0, 0.72);
}

body.dark-mode .cross-dr-status-pill--success {
  --cross-dr-status-bg: rgba(56, 158, 13, 0.16);
  --cross-dr-status-border: rgba(82, 196, 26, 0.42);
  --cross-dr-status-dot: #73d13d;
  --cross-dr-status-text: #b7eb8f;
}

body.dark-mode .cross-dr-status-pill--warning {
  --cross-dr-status-bg: rgba(212, 136, 6, 0.16);
  --cross-dr-status-border: rgba(250, 173, 20, 0.42);
  --cross-dr-status-dot: #ffc53d;
  --cross-dr-status-text: #ffe58f;
}

body.dark-mode .cross-dr-status-pill--error {
  --cross-dr-status-bg: rgba(207, 19, 34, 0.18);
  --cross-dr-status-border: rgba(255, 77, 79, 0.44);
  --cross-dr-status-dot: #ff7875;
  --cross-dr-status-text: #ffa39e;
}

body.dark-mode .cross-dr-status-pill--info {
  --cross-dr-status-bg: rgba(9, 109, 217, 0.18);
  --cross-dr-status-border: rgba(64, 169, 255, 0.44);
  --cross-dr-status-dot: #69c0ff;
  --cross-dr-status-text: #bae7ff;
}

body.dark-mode .cross-dr-status-pill--neutral {
  --cross-dr-status-bg: rgba(255, 255, 255, 0.08);
  --cross-dr-status-border: rgba(255, 255, 255, 0.16);
  --cross-dr-status-dot: rgba(255, 255, 255, 0.45);
  --cross-dr-status-text: rgba(255, 255, 255, 0.72);
}
</style>
