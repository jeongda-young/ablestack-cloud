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
  <div class="cross-dr-topology-tab">
    <dr-topology :plan="plan" :sourceSite="sourceSite" :targetSite="targetSite" />
  </div>
</template>

<script>
import DrTopology from '@/components/dr/DrTopology.vue'

export default {
  name: 'DrPlanTopologyTab',
  components: { DrTopology },
  props: {
    plan: { type: Object, required: true },
    sourceSite: { type: Object, default: () => ({}) },
    targetSite: { type: Object, default: () => ({}) }
  }
}
</script>

<style lang="less">
.cross-dr-topology-tab {
  width: 100%;
  min-width: 0;
}
</style>
