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
  <a-modal
    class="cross-dr-modal"
    :visible="visible"
    :closable="true"
    :maskClosable="false"
    :width="width"
    :bodyStyle="{ padding: 0 }"
    centered
    @cancel="$emit('cancel')">
    <template #title>
      <slot name="title">{{ title }}</slot>
    </template>
    <a-spin :spinning="loading || confirmLoading">
      <div class="cross-dr-modal__scroll">
        <slot />
      </div>
    </a-spin>
    <template #footer>
      <div class="cross-dr-modal__footer">
        <a-button @click="$emit('cancel')">{{ cancelText || $t('label.cancel') }}</a-button>
        <a-button
          type="primary"
          :danger="danger"
          :disabled="okDisabled"
          :loading="confirmLoading"
          @click="$emit('ok')">
          {{ okText || $t('label.ok') }}
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script>
export default {
  name: 'DrFormModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    confirmLoading: {
      type: Boolean,
      default: false
    },
    width: {
      type: [String, Number],
      default: 600
    },
    danger: {
      type: Boolean,
      default: false
    },
    okDisabled: {
      type: Boolean,
      default: false
    },
    okText: {
      type: String,
      default: ''
    },
    cancelText: {
      type: String,
      default: ''
    }
  }
}
</script>
