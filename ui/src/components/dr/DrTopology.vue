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
  <div class="cross-dr-topology">
    <div class="cross-dr-topology__node">
      <div class="cross-dr-topology__label">{{ $t('label.dr.source.site') }}</div>
      <div class="cross-dr-topology__name">{{ sourceName }}</div>
      <div class="cross-dr-topology__meta">{{ sourceMeta }}</div>
    </div>
    <div class="cross-dr-topology__link">
      <span class="cross-dr-topology__line"></span>
      <span class="cross-dr-topology__direction">{{ plan.direction || '-' }}</span>
      <span class="cross-dr-topology__line"></span>
    </div>
    <div class="cross-dr-topology__node">
      <div class="cross-dr-topology__label">{{ $t('label.dr.target.site') }}</div>
      <div class="cross-dr-topology__name">{{ targetName }}</div>
      <div class="cross-dr-topology__meta">{{ targetMeta }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DrTopology',
  props: {
    plan: {
      type: Object,
      required: true
    },
    sourceSite: {
      type: Object,
      default: () => ({})
    },
    targetSite: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    sourceName () {
      return this.sourceSite.name || this.plan.sourcesiteid || '-'
    },
    targetName () {
      return this.targetSite.name || this.plan.targetsiteid || '-'
    },
    sourceMeta () {
      return [this.sourceSite.sitetype, this.sourceSite.hypervisortype].filter(Boolean).join(' / ') || this.plan.sourceexternalref || '-'
    },
    targetMeta () {
      return [this.targetSite.sitetype, this.targetSite.hypervisortype].filter(Boolean).join(' / ') || '-'
    }
  }
}
</script>

<style lang="less">
.cross-dr-topology {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 280px) minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  width: 100%;
}

.cross-dr-topology__node {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--cross-dr-border, #e8e8e8);
  border-radius: 6px;
  background: var(--cross-dr-surface-muted, #fafafa);
}

.cross-dr-topology__label,
.cross-dr-topology__meta {
  color: var(--cross-dr-text-secondary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
  line-height: 18px;
}

.cross-dr-topology__name {
  overflow-wrap: anywhere;
  color: var(--cross-dr-text, rgba(0, 0, 0, 0.85));
  font-weight: 600;
  line-height: 22px;
}

.cross-dr-topology__link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
}

.cross-dr-topology__line {
  flex: 1;
  height: 1px;
  min-width: 24px;
  background: var(--cross-dr-border, #d9d9d9);
}

.cross-dr-topology__direction {
  max-width: 160px;
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--cross-dr-surface-muted, #fafafa);
  color: var(--cross-dr-text, rgba(0, 0, 0, 0.85));
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  overflow-wrap: anywhere;
}

body.dark-mode .cross-dr-topology {
  --cross-dr-border: rgba(255, 255, 255, 0.12);
  --cross-dr-surface-muted: rgba(255, 255, 255, 0.06);
  --cross-dr-text: rgba(255, 255, 255, 0.86);
  --cross-dr-text-secondary: rgba(255, 255, 255, 0.58);
}

@media (max-width: 760px) {
  .cross-dr-topology {
    grid-template-columns: 1fr;
  }

  .cross-dr-topology__link {
    justify-content: flex-start;
  }
}
</style>
