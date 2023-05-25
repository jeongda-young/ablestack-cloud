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
  <a-spin :spinning="loading">
    <a-tabs
      :activeKey="currentTab"
      :tabPosition="device === 'mobile' ? 'top' : 'left'"
      :animated="false"
      @change="handleChangeTab">
      <a-tab-pane :tab="$t('label.details')" key="details">
        <DetailsTab :resource="resource" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.access')" key="access">
        <!-- <a-card :title="$t('label.nfs')" :loading="versionLoading">
          <div v-if="NFSConfig !== ''">
            <a-textarea :value="NFSConfig" :rows="5" readonly />
            <div :span="24" class="action-button">
            </div>
          </div>
          <div v-else>
            <p>{{ $t('message.kubeconfig.cluster.not.available') }}</p>
          </div>
        </a-card> -->
        <a-card :title="$t('label.nfs')" :loading="versionLoading">
          <a-timeline>
            <a-timeline-item>
              <p v-html="$t('label.nfs.service')"></p>
                <p v-html="$t('label.nfs.Package')"></p>
                <p>sudo dnf install -y nfs-utils nfs4-acl-tools</p>
            </a-timeline-item>
            <a-timeline-item>
            <p v-html="$t('label.nfs.server.service')"></p>
                <p>mkdir -p /var/nfs/share</p>
                <p>echo "/var/nfs/share 10.10.254.136(rw,sync,no_subtree_check)" >> /etc/exports</p>
                <p>echo "/home 10.10.254.136(rw,sync,no_root_squash,no_subtree_check)" >> /etc/exports</p>
                <p>echo "/dev/sdb1 /data1 10.10.254.136(rw,sync,no_subtree_check)" >> /etc/exports</p>
                <p>exportfs -arv</p>
            </a-timeline-item>
            <a-timeline-item>
             <p v-html="$t('label.use.nfs.reboot')"></p>
              <p>
                <code><b>systemctl enable nfs-server.service</b></code>
                <code><b>systemctl start nfs-server.service</b></code>
              </p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
        <a-card :title="$t('label.nfs.client')">
          <a-timeline>
            <a-timeline-item>
              <p>
                {{ $t('label.nfs-utils.nfs4-acl-tools') }}<br><br>
                <code><b>sudo dnf install nfs-utils nfs4-acl-tools -y</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                {{ $t('label.nfs.mount.dir') }}<br><br>
                <code><b>sudo mkdir -p /nfs/share</b></code>
                  <code><b>sudo mkdir -p /nfs/home</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                {{ $t('label.nfs.mount') }}<br><br>
                <code><b>sudo mount host_ip:/var/nfs/share /nfs/share</b></code>
                <code><b>sudo mount host_ip:/home /nfs/home</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                {{ $t('label.nfs.test.file') }}<br><br>
                <code><b>sudo touch /nfs/share/test.txt</b></code>
                <code><b>sudo touch /nfs/home/home.txt</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p>
                {{ $t('label.nfs.client.fstab') }}<br><br>
                <code><b>echo "host_ip:/var/nfs/share /nfs/share nfs auto,nofail,noatime,nolock,intr,tcp,actimeo=1800 0 0" /etc/fstab</b></code><br><br>
                <code><b>echo "host_ip:/home /nfs/home nfs auto,nofail,noatime,nolock,intr,tcp,actimeo=1800 0 0" /etc/fstab</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
            <p>{{ $t('label.nfs.client.etc') }}</p>
              <p>
                {{ $t('label.nfs.mount') }}: <code><b>mount_nfs</b></code><br>
                {{ $t('label.nfs.fstab') }}: <code><b>set_fstab</b></code><br>
                {{ $t('label.nfs.umount') }}: <code><b>umount_nfs</b></code>
              </p>
             </a-timeline-item>
          </a-timeline>
        </a-card>
        <a-card :title="$t('label.disk.mount')">
        <p v-html="$t('label.mount.parted')"></p>
          <code><b>parted /dev/sdb mklabel gpt mkpart primary 0% 100%</b></code>
        <p v-html="$t('label.mkfs.mount')"></p>
          <code><b>sudo mkfs.xfs /dev/sdb1</b></code>
          <code><b>sudo mkdir /data1</b></code>
          <code><b>sudo mount /dev/sdb1 /data1</b></code>
        <p v-html="$t('label.mkfs.mount')"></p>
          <code><b>echo "/mnt/data *(rw,sync,no_root_squash)" >> /etc/exports</b></code>
          <code><b>exportfs -a</b></code>
        <p v-html="$t('label.nfs.restart')"></p>
          <code><b>systemctl start nfs-utils</b></code>
          <code><b>systemctl enable nfs-utils</b></code>
          <code><b>systemctl restart nfs-utils</b></code>
        </a-card>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.networks')" key="networks" >
        <SsvNicsTab :resource="networks" :loading="loading"/>
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.volumes')" key="volumes">
        <SsvVolumesTab :resource="sharedstoragevm" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane :tab="$t('label.instance')" key="sharedstoragevm">
        <a-table
          class="table"
          size="small"
          :columns="ssvVmColumns"
          :dataSource="sharedstoragevm"
          :rowKey="item => item.id"
          :pagination="false"
        >
          <template #name="{ text, record }" :name="text">
            <router-link :to="{ path: '/vm/' + record.id }">{{ record.name }}</router-link>
          </template>
          <template #state="{ text }">
            <status :text="text ? text : ''" displayText />
          </template>
          <template #instancename="{text}">
            <status :text="text ? text : ''" />{{ text }}
          </template>
          <template #ipaddress="{text}">
            <status :text="text ? text : ''" />{{ text }}
          </template>
          <template #hostname="{record}">
            <router-link :to="{ path: '/host/' + record.hostid }">{{ record.hostname }}</router-link>
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
      sharedstoragevm: {},
      networkLoading: false,
      networks: [],
      networkip: '',
      currentTab: 'details',
      instanceLoading: false,
      virtualmachines: [],
      ssvVmColumns: [],
      annotations: []
    }
  },
  created () {
    this.ssvVmColumns = [
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
        title: this.$t('label.instancename'),
        dataIndex: 'instancename'
      },
      // {
      //   title: this.$t('label.ip'),
      //   dataIndex: 'ipaddress'
      // },
      {
        title: this.$t('label.hostid'),
        dataIndex: 'hostname',
        slots: { customRender: 'hostname' }
      },
      {
        title: this.$t('label.zonename'),
        dataIndex: 'zonename'
      }
    ]
    if (!isAdmin()) {
      this.ssvVmColumns = this.ssvVmColumns.filter(x => x.dataIndex !== 'sharedstoragevm')
    }
    this.handleFetchData()
    const self = this
    window.addEventListener('popstate', function () {
      self.setCurrentTab()
    })
    const userInfo = this.$store.getters.userInfo
    if (!['Admin'].includes(userInfo.roletype) &&
      (userInfo.account !== this.resource.account || userInfo.domain !== this.resource.domain)) {
      this.ssvVmColumns = this.ssvVmColumns.filter(col => { return col.dataIndex !== 'hostname' })
    }
    this.sharedstoragevm = this.resource
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.sharedstoragevm = newItem
      this.fetchData()
    },
    $route: function (newItem, oldItem) {
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
      this.sharedstoragevm = this.resource.sharedstoragevm || []
      this.sharedstoragevm.map(x => { x.ipaddress = x.nic[0].ipaddress })
      this.networks = this.resource.network

      if (!this.sharedstoragevm || !this.sharedstoragevm.id) {
        // return
      }
      // api('listNetworks', { id: this.resource.networkid }).then(json => {
      //   console.log(json.listnetworksresponse.network)
      //   this.ssvnetworks = json.listnetworksresponse.network
      //   if (this.ssvnetworks) {
      //     this.ssvnetworks.sort((a, b) => { return a.deviceid - b.deviceid })
      //   }
      //   // this.$set(this.resource, 'desktopnetworks', this.desktopnetworks)
      // }).finally(() => {
      // })
    },
    fetchInstances () {
      this.instanceLoading = true
      this.sharedstoragevm = this.resource.sharedstoragevm || []
      this.sharedstoragevm.map(x => { x.ipaddress = x.nic[0].ipaddress })
      this.instanceLoading = false
    },
    // fetchPublicIpAddress () {
    //   this.networkLoading = true
    //   var params = {
    //     listAll: true,
    //     forvirtualnetwork: true
    //   }
    //   if (!this.isObjectEmpty(this.resource)) {
    //     if (this.isValidValueForKey(this.resource, 'projectid') &&
    //       this.resource.projectid !== '') {
    //       params.projectid = this.resource.projectid
    //     }
    //     if (this.isValidValueForKey(this.resource, 'networkid')) {
    //       params.associatednetworkid = this.resource.networkid
    //     }
    //   }
    //   if (this.resource.networkid !== undefined) {
    //     api('listPublicIpAddresses', params).then(json => {
    //       let ips = json.listpublicipaddressesresponse.publicipaddress
    //       if (this.arrayHasItems(ips)) {
    //         ips = ips.filter(x => x.issourcenat)
    //         this.publicIpAddress = ips.length > 0 ? ips[0] : null
    //       }
    //     }).catch(error => {
    //       this.$notifyError(error)
    //     }).finally(() => {
    //       this.networkLoading = false
    //     })
    //   }
    // },
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
