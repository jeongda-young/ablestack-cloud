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
        <div v-if="resource.sharedstoragevmtype =='NFS'">
          <a-card :title="$t('label.nfs.host')" :loading="versionLoading">
            <a-timeline>
              <a-timeline-item>
                  <p v-html="$t('label.nfs.package')"></p>
                  <code><b>sudo dnf install -y nfs-utils nfs4-acl-tools</b></code>
              </a-timeline-item>
              <a-timeline-item>
              <p v-html="$t('label.nfs.server.service')"></p>
                  <code><b>mkdir -p /var/nfs/share</b></code>
                  <code><b>echo "/var/nfs/share 10.10.254.136(rw,sync,no_subtree_check)" >> /etc/exports</b></code><br>
                  <code><b>echo "/home 10.10.254.136(rw,sync,no_root_squash,no_subtree_check)" >> /etc/exports</b></code><br>
                  <code><b>echo "/dev/sdb1 /data1 10.10.254.136(rw,sync,no_subtree_check)" >> /etc/exports</b></code><br>
                  <code><b>systemctl enable nfs-server.service</b></code>
              </a-timeline-item>
              <a-timeline-item>
                <p v-html="$t('label.nfs.firewall')"></p>
                <code><b>firewall-cmd --zone=public --permanent --add-service={nfs,mountd,rpc-bind}</b></code><br>
                <code><b>firewall-cmd --reload</b></code>
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
                  {{ $t('label.nfs.umount') }}: <code><b>umount_nfs</b></code>
                </p>
              </a-timeline-item>
            </a-timeline>
          </a-card>
          <a-card :title="$t('label.disk.mount')">
            <a-timeline>
              <a-timeline-item>
          <p v-html="$t('label.mount.parted')"></p>
            <code><b>parted /dev/sdb mklabel gpt mkpart primary 0% 100%</b></code><br><br>
              </a-timeline-item>
              <a-timeline-item>
          <p v-html="$t('label.nfs.mount')"></p>
            <code><b>sudo mkfs.xfs /dev/sdb1</b></code><br>
            <code><b>sudo mkdir /data1</b></code><br>
            <code><b>sudo mount /dev/sdb1 /data1</b></code><br><br>
            </a-timeline-item>
            <a-timeline-item>
          <p v-html="$t('label.mkfs.mount')"></p>
            <code><b>echo "/mnt/data *(rw,sync,no_root_squash)" >> /etc/exports</b></code><br>
            <code><b>exportfs -a</b></code><br><br>
            </a-timeline-item>
            <a-timeline-item>
          <p v-html="$t('label.nfs.restart')"></p>
            <code><b>systemctl start nfs-utils</b></code><br>
            <code><b>systemctl enable nfs-utils</b></code><br>
            <code><b>systemctl restart nfs-utils</b></code>
            </a-timeline-item>
            </a-timeline>
          </a-card>
      </div>
      <div v-if="resource.sharedstoragevmtype =='SMB'">
        <a-card :title="$t('label.smb.host')" :loading="versionLoading">
          <a-timeline>
            <a-timeline-item>
                <p v-html="$t('label.smb.package')"></p>
                <code><b>dnf -y install samba</b></code>
            </a-timeline-item>
            <a-timeline-item>
            <p v-html="$t('label.nfs.folder')"></p>
                <code><b>mkdir -p /mnt/sdb1/smb</b></code>
                <code><b>chmod -R 777 /mnt/sdb1/smb</b></code><br>
            </a-timeline-item>
            <a-timeline-item>
            <p v-html="$t('label.smb.user')"></p>
              <p>
                <code><b>useradd user1</b></code><br>
                <code><b>echo 'user1' | passwd --stdin user1</b></code>
              </p>
            </a-timeline-item>
            <a-timeline-item>
              <p v-html="$t('label.smb.user.create')"></p>
                <p>
                  <code><b>echo -e 'user1\nuser1' | smbpasswd -s -a user1</b></code><br>
                  <code><b>echo 'user1' | passwd --stdin user1</b></code>
                </p>
              </a-timeline-item>
              <a-timeline-item>
                <p v-html="$t('label.smb.user.settings')"></p>
                  <p>
                    <code><b>touch /etc/samba/smb.conf</b></code><br>
                    <code><b>echo "</b></code><br>
                    <code><b>[user1]</b></code><br>
                    <code><b>path = /mnt/sdb1/smb</b></code><br>
                    <code><b>browseable = yes</b></code><br>
                    <code><b>writable = yes</b></code><br>
                    <code><b>write list = user1</b></code><br>
                    <code><b>force create mode = 0777</b></code><br>
                    <code><b>force directory mode = 2770</b></code><br>
                    <code><b>public = yes" >> /etc/samba/smb.conf</b></code>
                  </p>
                </a-timeline-item>
                <a-timeline-item>
                  <p v-html="$t('label.smb.selinux.settings')"></p>
                    <p>
                      <code><b>setsebool -P samba_enable_home_dirs on</b></code><br>
                      <code><b>setsebool -P samba_export_all_rw on </b></code><br>
                      <code><b>chcon -R -t samba_share_t /mnt/sdb1/smb </b></code>
                    </p>
                  </a-timeline-item>
                  <a-timeline-item>
                    <p v-html="$t('label.smb.firewall')"></p>
                      <p>
                        <code><b>firewall-cmd --zone=public --permanent --add-service=samba</b></code><br>
                        <code><b>firewall-cmd --reload </b></code>
                      </p>
                    </a-timeline-item>
                  <a-timeline-item>
                    <p v-html="$t('label.smb.service.start')"></p>
                      <p>
                        <code><b>systemctl enable smb</b></code><br>
                        <code><b>systemctl start smb </b></code>
                      </p>
                    </a-timeline-item>
          </a-timeline>
        </a-card>
        <a-card :title="$t('label.disk.mount')">
          <a-timeline>
            <a-timeline-item>
        <p v-html="$t('label.mount.parted')"></p>
          <code><b>parted /dev/sdb mklabel gpt mkpart primary 0% 100%</b></code><br><br>
            </a-timeline-item>
            <a-timeline-item>
        <p v-html="$t('label.nfs.mount')"></p>
          <code><b>sudo mkfs.xfs /dev/sdb1</b></code><br>
          <code><b>sudo mkdir /data1</b></code><br>
          <code><b>sudo mount /dev/sdb1 /data1</b></code><br><br>
          </a-timeline-item>
          <a-timeline-item>
        <p v-html="$t('label.mkfs.mount')"></p>
          <code><b>echo "/mnt/data *(rw,sync,no_root_squash)" >> /etc/exports</b></code><br>
          <code><b>exportfs -a</b></code><br><br>
          </a-timeline-item>
          <a-timeline-item>
        <p v-html="$t('label.smb.restart')"></p>
          <code><b>systemctl start smb-utils</b></code><br>
          <code><b>systemctl enable smb-utils</b></code><br>
          <code><b>systemctl restart smb-utils</b></code>
          </a-timeline-item>
          </a-timeline>
        </a-card>
    </div>
    <div v-if="resource.sharedstoragevmtype =='ISCSI'">
      <a-card :title="$t('label.iscsi.host')" :loading="versionLoading">
        <a-timeline>
          <a-timeline-item>
              <p v-html="$t('label.iscsi.package')"></p>
              <code><b>dnf install -y targetcli xfsprogs</b></code>
          </a-timeline-item>
          <a-timeline-item>
          <p v-html="$t('label.iscsi.firewall')"></p>
              <code><b>firewall-cmd --zone=public --permanent --add-port=3260/tcp</b></code><br>
              <code><b>firewall-cmd --reload</b></code>
          </a-timeline-item>
          <a-timeline-item>
          <p v-html="$t('label.iscsi.targetcli')"></p>
            <p>
              <code><b>targetcli /backstores/block create iscsi_store /dev/sdb1</b></code><br>
              <code><b>targetcli /iscsi create ${TG_IQN_NAME}</b></code><br>
              <code><b>targetcli /iscsi/${TG_IQN_NAME}/tpg1/luns create /backstores/block/iscsi_store</b></code><br>
              <code><b>targetcli /iscsi/${TG_IQN_NAME}/tpg1/acls create ${IN_IQN_NAME}</b></code>
            </p>
          </a-timeline-item>
          <a-timeline-item>
            <p v-html="$t('label.iscsi.start')"></p>
                <code><b>systemctl start target</b></code><br>
                <code><b>systemctl enable target</b></code>
            </a-timeline-item>
        </a-timeline>
      </a-card>
      <a-card :title="$t('label.iscsi.client')">
        <a-timeline>
          <a-timeline-item>
            <p>
              {{ $t('label.iscsi.iscsiadm.discovery') }}<br><br>
              <code><b>iscsiadm -m discovery -t st -p 10.10.254.140</b></code>
            </p>
          </a-timeline-item>
          <a-timeline-item>
            <p>
              {{ $t('label.iscsi.initiator') }}<br><br>
              <code><b>echo "InitiatorName=iqn.2023-04.com.example:storage.initiator01" > /etc/iscsi/initiatorname.iscsi</b></code>
            </p>
          </a-timeline-item>
          <a-timeline-item>
            <p>
              {{ $t('label.iscsi.service.start') }}<br><br>
              <code><b>systemctl enable iscsid</b></code><br>
              <code><b>systemctl restart iscsid</b></code>
            </p>
          </a-timeline-item>
          <a-timeline-item>
            <p>
              {{ $t('label.iscsi.connect.settings') }}<br><br>
              <code><b>iscsiadm -m node -T ${TG_IQN_NAME} -p ${ISCSI_SERVER_IP}:3260,1 --login</b></code>
            </p>
          </a-timeline-item>
          <a-timeline-item>
              <p>
                {{ $t('label.iscsi.mount.format') }}<br><br>
                 <code><b>mkdir /mnt/iscsi_store</b></code><br>
                 <code><b>mkfs -t ext4 /dev/sdc</b></code><br>
                 <code><b>mount /dev/sdc /mnt/iscsi_store</b></code>
              </p>
            </a-timeline-item>
          <a-timeline-item>
            <p>
              {{ $t('label.iscsi.client.fstab') }}<br><br>
              <code><b>echo "/dev/sdc /mnt/iscsi_store/ xfs _netdev 0 0" | sudo tee -a /etc/fstab</b></code>
            </p>
          </a-timeline-item>
        </a-timeline>
      </a-card>
      <a-card :title="$t('label.disk.mount')">
        <a-timeline>
          <a-timeline-item>
      <p v-html="$t('label.mount.parted')"></p>
        <code><b>parted /dev/sdb mklabel gpt mkpart primary 0% 100%</b></code><br><br>
          </a-timeline-item>
          <a-timeline-item>
      <p v-html="$t('label.nfs.mount')"></p>
        <code><b>sudo mkfs.xfs /dev/sdb1</b></code><br>
        <code><b>sudo mkdir /data1</b></code><br>
        <code><b>sudo mount /dev/sdb1 /data1</b></code><br><br>
        </a-timeline-item>
        <a-timeline-item>
      <p v-html="$t('label.mkfs.mount')"></p>
        <code><b>echo "/mnt/data *(rw,sync,no_root_squash)" >> /etc/exports</b></code><br>
        <code><b>exportfs -a</b></code><br><br>
        </a-timeline-item>
        <a-timeline-item>
      <p v-html="$t('label.iscsi.restart')"></p>
        <code><b>systemctl start iscsi-utils</b></code><br>
        <code><b>systemctl enable iscsi-utils</b></code><br>
        <code><b>systemctl restart iscsi-utils</b></code>
        </a-timeline-item>
        </a-timeline>
      </a-card>
  </div>
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
