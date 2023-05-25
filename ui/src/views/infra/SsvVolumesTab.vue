// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-table
    class="table"
    size="small"
    :columns="volumeColumns"
    :dataSource="volumes"
    :pagination="false"
  >
    <template #name="{ text, record }">
      <hdd-outlined style="margin-right: 5px"/>
      <router-link :to="{ path: '/volume/' + record.id }" style="margin-right: 5px">
        {{ text }}
      </router-link>
      <a-tag v-if="record.provisioningtype">
        {{  record.provisioningtype  }}
      </a-tag>
    </template>
    <template #state="{ text }">
      <status :text="text ? text : ''" />{{ text }}
    </template>
    <template #size="{ record }">
      {{ parseFloat(record.size / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GB
    </template>
  </a-table>
</template>

<script>
import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'SsvVolumesTab',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      sharedstoragevm: [],
      volumes: [],
      volumeColumns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name',
          slots: { customRender: 'name' }
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state',
          slots: { customRender: 'state' }
        },
        {
          title: this.$t('label.type'),
          dataIndex: 'type'
        },
        {
          title: this.$t('label.size'),
          dataIndex: 'size',
          slots: { customRender: 'size' }
        }
      ]
    }
  },
  created () {
    this.sharedstoragevm = this.resource
    this.fetchData()
    // console.log(this.sharedstoragevm[0].id)
  },
  // watch: {
  //   resource: function (newItem) {
  //     this.sharedstoragevm = newItem
  //     this.fetchData()
  //   }
  // },
  methods: {
    fetchData () {
      this.volumes = []
      if (!this.sharedstoragevm[0].id) {
        return false
      } else {
        this.getVolumes()
      }
    },
    getVolumes () {
      api('listVolumes', { listall: true, listsystemvms: true, virtualmachineid: this.sharedstoragevm[0].id }).then(json => {
        // console.log(json.listvolumesresponse.volume)
        this.volumes = json.listvolumesresponse.volume
        if (this.volumes) {
          this.volumes.sort((a, b) => { return a.deviceid - b.deviceid })
        }
      })
    }
  }
}
</script>
