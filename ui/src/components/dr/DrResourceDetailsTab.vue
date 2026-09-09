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
  <a-list
    size="small"
    :dataSource="visibleFields">
    <template #renderItem="{ item }">
      <a-list-item>
        <div class="dr-standard-detail-row">
          <strong>{{ item.label }}</strong>
          <br />
          <component
            v-if="item.component"
            :is="item.component"
            v-bind="item.props || {}" />
          <router-link
            v-else-if="item.route"
            :to="item.route">
            {{ formatValue(item.value) }}
          </router-link>
          <span v-else>{{ formatValue(item.value) }}</span>
        </div>
      </a-list-item>
    </template>
  </a-list>
</template>

<script>
export default {
  name: 'DrResourceDetailsTab',
  props: {
    resource: {
      type: Object,
      default: () => ({})
    },
    fields: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    visibleFields () {
      return this.fields.filter(field => field && field.visible !== false)
    }
  },
  methods: {
    formatValue (value) {
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      return value
    }
  }
}
</script>
