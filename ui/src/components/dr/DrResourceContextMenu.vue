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
  <div
    v-if="visible"
    ref="menu"
    class="quickview-context-menu"
    :style="{ top: adjustedPosition.y + 'px', left: adjustedPosition.x + 'px' }"
    @click.stop
    @contextmenu.stop.prevent>
    <dr-action-menu-content
      :actions="actions"
      :resource="resource"
      :show-resource-title="showResourceTitle"
      :title="title"
      @exec-action="execAction" />
  </div>
</template>

<script>
import DrActionMenuContent from '@/components/dr/DrActionMenuContent.vue'

export default {
  name: 'DrResourceContextMenu',
  components: {
    DrActionMenuContent
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    actions: {
      type: Array,
      default: () => []
    },
    resource: {
      type: Object,
      default: () => ({})
    },
    position: {
      type: Object,
      default: () => ({ x: 0, y: 0 })
    },
    title: {
      type: String,
      default: ''
    },
    showResourceTitle: {
      type: Boolean,
      default: true
    }
  },
  emits: ['exec-action', 'close'],
  data () {
    return {
      adjustedPosition: { x: 0, y: 0 },
      listenerRegistered: false
    }
  },
  watch: {
    visible (value) {
      if (value) {
        this.adjustedPosition = Object.assign({}, this.position)
        this.$nextTick(this.adjustPosition)
        this.addListeners()
      } else {
        this.removeListeners()
      }
    },
    position: {
      deep: true,
      handler (value) {
        this.adjustedPosition = Object.assign({}, value)
        this.$nextTick(this.adjustPosition)
      }
    }
  },
  beforeUnmount () {
    this.removeListeners()
  },
  methods: {
    addListeners () {
      if (this.listenerRegistered) {
        return
      }
      document.addEventListener('click', this.close)
      this.listenerRegistered = true
    },
    removeListeners () {
      if (!this.listenerRegistered) {
        return
      }
      document.removeEventListener('click', this.close)
      this.listenerRegistered = false
    },
    close () {
      this.$emit('close')
    },
    execAction (action) {
      this.close()
      this.$emit('exec-action', action, action.resource || this.resource)
    },
    adjustPosition () {
      const menu = this.$refs.menu
      if (!menu) {
        return
      }
      const padding = 8
      const rect = menu.getBoundingClientRect()
      const maxX = window.innerWidth - rect.width - padding
      const maxY = window.innerHeight - rect.height - padding
      this.adjustedPosition = {
        x: Math.max(padding, Math.min(this.adjustedPosition.x, maxX)),
        y: Math.max(padding, Math.min(this.adjustedPosition.y, maxY))
      }
    }
  }
}
</script>
