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
  <a-spin :spinning="networkLoading">
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('label.details')" key="details">
        <DetailsTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.access')" key="access">
        <a-card :title="$t('label.cluster')" :loading="versionLoading">
        </a-card>
        <a-card :title="$t('label.using.cli')" :loading="versionLoading">
        </a-card>
        <a-card :title="$t('label.dashboard')">
        </a-card>
        <a-card :title="$t('label.access.nodes')">
        </a-card>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.networks')" key="networks" >
        <SsvNicsTab :resource="networks" :loading="loading"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.volumes')" key="volumes" >
        <SsvVolumesTab :resource="volumes" :loading="loading"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.instance')" key="sharedstoragevm">
        <a-table
          class="table"
          size="small"
          :columns="vmColumns"
          :dataSource="virtualmachines"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template #name="{ text, record }" :name="text">
            <router-link :to="{ path: '/vm/' + record.id }">{{ record.name }}</router-link>
          </template>
          <template #state="{ text }">
            <status :text="text ? text : ''" displayText />
          </template>
          <template #port="{ text, record, index }" :name="text" :record="record">
            {{ cksSshStartingPort + index }}
          </template>
          <template #action="{record}">
            <a-tooltip placement="bottom" >
              <template #title>
                {{ $t('label.action.delete.node') }}
              </template>
              <a-popconfirm
                :title="$t('message.action.delete.node')"
                @confirm="deleteNode(record)"
                :okText="$t('label.yes')"
                :cancelText="$t('label.no')"
                :disabled="!['Created', 'Running'].includes(resource.state) || resource.autoscalingenabled"
              >
                <a-button
                  danger
                  type="primary"
                  shape="circle"
                  :disabled="!['Created', 'Running'].includes(resource.state) || resource.autoscalingenabled">
                  <template #icon><delete-outlined /></template>
                </a-button>
              </a-popconfirm>
            </a-tooltip>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { isAdmin } from '@/role'
import { mixinDevice } from '@/utils/mixin.js'
import DetailsTab from '@/components/view/DetailsTab'
import Status from '@/components/widgets/Status'
import AnnotationsTab from '@/components/view/AnnotationsTab'
import SsvVolumesTab from '@/views/infra/SsvVolumesTab.vue'
import SsvNicsTab from '@/views/infra/SsvNicsTab'

export default {
  name: 'SsvTabs',
  components: {
    DetailsTab,
    Status,
    AnnotationsTab,
    SsvVolumesTab,
    SsvNicsTab
  },
  mixins: [mixinDevice],
  inject: ['parentFetchData'],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      versionLoading: false,
      sharedstoragevm: false,
      networkLoading: false,
      networks: [],
      volumes: [],
      networkip: null,
      currentTab: 'details',
      annotations: []
    }
  },
  created () {
    this.vmColumns = [
      {
        title: this.$t('label.name'),
        dataIndex: 'displayname',
        slots: { customRender: 'displayname' }
      },
      {
        title: this.$t('label.state'),
        dataIndex: 'state',
        slots: { customRender: 'state' }
      },
      {
        title: this.$t('label.instancename'),
        dataIndex: 'sharedstoragevm'
      },
      {
        title: this.$t('label.zonename'),
        dataIndex: 'zonename'
      }
    ]
    if (!isAdmin()) {
      this.vmColumns = this.vmColumns.filter(x => x.dataIndex !== 'sharedstoragevm')
    }
    this.handleFetchData()
    const self = this
    window.addEventListener('popstate', function () {
      self.setCurrentTab()
    })
  },
  watch: {
    resource: {
      deep: true,
      handler (newData, oldData) {
        if (newData && newData !== oldData) {
          this.handleFetchData()
          if (this.resource.ipaddress) {
            this.vmColumns = this.vmColumns.filter(x => x.dataIndex !== 'ipaddress')
          } else {
            this.vmColumns = this.vmColumns.filter(x => x.dataIndex !== 'port')
          }
        }
      }
    },
    '$route.fullPath': function () {
      this.setCurrentTab()
    }
  },
  mounted () {
    this.handleFetchData()
    this.setCurrentTab()
  },
  methods: {
    setCurrentTab () {
      this.currentTab = this.$route.query.tab ? this.$route.query.tab : 'details'
    },
    handleChangeTab (e) {
      this.currentTab = e
      const query = Object.assign({}, this.$route.query)
      query.tab = e
      history.pushState(
        {},
        null,
        '#' + this.$route.path + '?' + Object.keys(query).map(key => {
          return (
            encodeURIComponent(key) + '=' + encodeURIComponent(query[key])
          )
        }).join('&')
      )
    },
    isValidValueForKey (obj, key) {
      return key in obj && obj[key] != null
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    isObjectEmpty (obj) {
      return !(obj !== null && obj !== undefined && Object.keys(obj).length > 0 && obj.constructor === Object)
    },
    handleFetchData () {
      this.fetchInstances()
    },
    fetchData () {
      this.networks = []
      this.iprange = []
      if (!this.vm || !this.vm.id) {
        return
      }
      api('listNetworks', { id: this.resource.networkid }).then(json => {
        this.networks = json.listnetworksresponse.network
        if (this.networks) {
          this.networks.sort((a, b) => { return a.deviceid - b.deviceid })
        }
      }).finally(() => {
      })
    },
    fetchDiskOfferings (zoneId) {
      api('listDiskOfferings', {
        zoneid: zoneId,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
        if (!this.createVolumeFromSnapshot) {
          this.form.diskofferingid = this.offerings[0].id || ''
        }
        this.customDiskOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      })
    },
    fetchInstances () {
      this.sharedstoragevm = true
      this.virtualmachines = this.resource.virtualmachines || []
      this.virtualmachines.map(x => { x.ipaddress = x.nic[0].ipaddress })
      this.sharedstoragevm = false
    },
    listNetworks () {
      api('listNetworks', {
        listAll: 'true',
        showicon: true,
        zoneid: this.vm.zoneid
      }).then(response => {
        this.addNetworkData.allNetworks = response.listnetworksresponse.network.filter(network => !this.vm.nic.map(nic => nic.networkid).includes(network.id))
        this.addNetworkData.network = this.addNetworkData.allNetworks[0].id
      })
    },
    deleteNode (node) {
      const params = {
        id: this.resource.id,
        nodeids: node.id
      }
      api('scaleKubernetesCluster', params).then(json => {
        const jobId = json.scalekubernetesclusterresponse.jobid
        console.log(jobId)
        this.$store.dispatch('AddAsyncJob', {
          title: this.$t('label.action.delete.node'),
          jobid: jobId,
          description: node.name,
          status: 'progress'
        })
        this.$pollJob({
          jobId,
          loadingMessage: `${this.$t('message.deleting.node')} ${node.name}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: `${this.$t('message.success.delete.node')} ${node.name}`,
          successMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.parentFetchData()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .list {

    &__item,
    &__row {
      display: flex;
      flex-wrap: wrap;
      width: 100%;
    }

    &__item {
      margin-bottom: -20px;
    }

    &__col {
      flex: 1;
      margin-right: 20px;
      margin-bottom: 20px;
    }

    &__label {
      font-weight: bold;
    }

  }

  .pagination {
    margin-top: 20px;
  }

  .table {
    margin-top: 20px;
    overflow-y: auto;
  }
</style>
