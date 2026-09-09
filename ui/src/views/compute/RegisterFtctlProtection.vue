<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<template>
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-spin :spinning="loading">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit">
        <a-form-item name="mode">
          <template #label>
            <tooltip-label :title="$t('label.mode')" :tooltip="$t('placeholder.ftctl.mode')" />
          </template>
          <a-select v-model:value="form.mode" v-focus="true" @change="handleModeChange">
            <a-select-option value="ha">HA</a-select-option>
            <a-select-option value="dr">DR</a-select-option>
            <a-select-option value="ft">FT</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="drpeersitetype" v-if="showDrPeerSiteType">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.dr.peer.site.type')" :tooltip="$t('placeholder.ftctl.dr.peer.site.type')" />
          </template>
          <a-radio-group v-model:value="form.drpeersitetype" @change="handleDrPeerSiteTypeChange">
            <a-radio-button value="local-mold">{{ $t('label.ftctl.dr.peer.site.local.mold') }}</a-radio-button>
            <a-radio-button value="remote-mold">{{ $t('label.ftctl.dr.peer.site.remote.mold') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item name="peerhostid" v-if="showLocalMoldPeerFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.peer.host.id')" :tooltip="$t('placeholder.ftctl.peer.host.id')" />
          </template>
          <a-select
            v-model:value="form.peerhostid"
            :loading="hostsLoading"
            :placeholder="$t('placeholder.ftctl.peer.host.id')"
            showSearch
            optionFilterProp="label"
            @change="handlePeerHostChange"
            :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0">
            <a-select-option
              v-for="host in hosts"
              :key="host.id"
              :value="host.id"
              :label="formatHostLabel(host)">
              {{ formatHostLabel(host) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="targetstoragepoolid" v-if="showLocalStorageFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.target.storage.scope')" :tooltip="$t('placeholder.ftctl.target.storage.scope')" />
          </template>
          <a-select
            v-model:value="form.targetstoragepoolid"
            :loading="storagePoolsLoading"
            :placeholder="$t('placeholder.ftctl.target.storage.pool')"
            showSearch
            optionFilterProp="label"
            @change="handleStoragePoolChange"
            :filterOption="(input, option) => option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0">
            <a-select-option
              v-for="pool in storagePools"
              :key="pool.id"
              :value="pool.id"
              :label="formatStoragePoolLabel(pool)">
              {{ formatStoragePoolLabel(pool) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="networkids" v-if="showLocalTargetNetworkFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.target.networks')" :tooltip="$t('placeholder.ftctl.target.networks')" />
          </template>
          <a-select
            v-model:value="form.networkids"
            mode="multiple"
            :loading="networksLoading"
            :placeholder="$t('placeholder.ftctl.target.networks')"
            showSearch
            optionFilterProp="label"
            :filterOption="filterRemoteInventoryOption">
            <a-select-option
              v-for="network in networks"
              :key="network.id"
              :value="network.id"
              :label="formatNetworkLabel(network)">
              {{ formatNetworkLabel(network) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="showRemoteMoldFields" class="ftctl-remote-mold-fields">
          <a-form-item name="remotemoldapiurl">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.mold.api.url')" :tooltip="$t('placeholder.ftctl.remote.mold.api.url')" />
            </template>
            <a-input v-model:value="form.remotemoldapiurl" placeholder="https://remote-mold.example.com/client/api" />
          </a-form-item>
          <a-form-item name="remotemoldapikey">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.mold.api.key')" :tooltip="$t('placeholder.ftctl.remote.mold.api.key')" />
            </template>
            <a-input v-model:value="form.remotemoldapikey" />
          </a-form-item>
          <a-form-item name="remotemoldsecretkey">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.mold.secret.key')" :tooltip="$t('placeholder.ftctl.remote.mold.secret.key')" />
            </template>
            <a-input-password v-model:value="form.remotemoldsecretkey" />
          </a-form-item>
          <div class="ftctl-remote-mold-actions">
            <a-button :loading="remoteMoldLoading" @click="validateRemoteMoldConnection">
              {{ $t('label.ftctl.remote.mold.test.connection') }}
            </a-button>
            <a-button :loading="remoteMoldHostsLoading || remoteMoldStoragePoolsLoading || remoteMoldNetworksLoading" @click="fetchRemoteMoldInventory">
              {{ $t('label.refresh') }}
            </a-button>
          </div>
          <a-form-item name="remotepeerhostuuid">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.peer.host')" :tooltip="$t('placeholder.ftctl.remote.peer.host')" />
            </template>
            <a-select
              v-model:value="form.remotepeerhostuuid"
              :loading="remoteMoldHostsLoading"
              :placeholder="$t('placeholder.ftctl.remote.peer.host')"
              showSearch
              optionFilterProp="label"
              @change="handleRemotePeerHostChange"
              :filterOption="filterRemoteInventoryOption">
              <a-select-option
                v-for="host in remoteMoldHosts"
                :key="host.id"
                :value="host.id"
                :label="formatHostLabel(host)">
                {{ formatHostLabel(host) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="remotetargetstoragepooluuid">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.target.storage.pool')" :tooltip="$t('placeholder.ftctl.remote.target.storage.pool')" />
            </template>
            <a-select
              v-model:value="form.remotetargetstoragepooluuid"
              :loading="remoteMoldStoragePoolsLoading"
              :placeholder="$t('placeholder.ftctl.remote.target.storage.pool')"
              showSearch
              optionFilterProp="label"
              @change="handleRemoteStoragePoolChange"
              :filterOption="filterRemoteInventoryOption">
              <a-select-option
                v-for="pool in remoteMoldStoragePools"
                :key="pool.id"
                :value="pool.id"
                :label="formatStoragePoolLabel(pool)">
                {{ formatStoragePoolLabel(pool) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="networkids" v-if="showRemoteTargetNetworkFields">
            <template #label>
              <tooltip-label :title="$t('label.ftctl.remote.target.networks')" :tooltip="$t('placeholder.ftctl.remote.target.networks')" />
            </template>
            <a-select
              v-model:value="form.networkids"
              mode="multiple"
              :loading="remoteMoldNetworksLoading"
              :placeholder="$t('placeholder.ftctl.remote.target.networks')"
              showSearch
              optionFilterProp="label"
              :filterOption="filterRemoteInventoryOption">
              <a-select-option
                v-for="network in remoteMoldNetworks"
                :key="network.id"
                :value="network.id"
                :label="formatNetworkLabel(network)">
                {{ formatNetworkLabel(network) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="remotepeersshoverride">
            <a-checkbox v-model:checked="remotePeerSshOverride" @change="handleRemotePeerSshOverrideChange">
              {{ $t('label.ftctl.remote.peer.ssh.override') }}
            </a-checkbox>
          </a-form-item>
          <a-form-item name="remotepeersshautosetup">
            <a-checkbox v-model:checked="remotePeerSshAutoSetup">
              {{ $t('label.ftctl.remote.peer.ssh.auto.setup') }}
            </a-checkbox>
          </a-form-item>
          <div v-if="remotePeerSshOverride" class="ftctl-remote-peer-ssh-fields">
            <a-form-item name="remotepeersshuser">
              <template #label>
                <tooltip-label :title="$t('label.ftctl.remote.peer.ssh.user')" :tooltip="$t('placeholder.ftctl.remote.peer.ssh.user')" />
              </template>
              <a-input v-model:value="form.remotepeersshuser" placeholder="root" @change="rebuildRemotePeerLibvirtUri" />
            </a-form-item>
            <a-form-item name="remotepeersshport">
              <template #label>
                <tooltip-label :title="$t('label.ftctl.remote.peer.ssh.port')" :tooltip="$t('placeholder.ftctl.remote.peer.ssh.port')" />
              </template>
              <a-input v-model:value="form.remotepeersshport" placeholder="22" @change="rebuildRemotePeerLibvirtUri" />
            </a-form-item>
            <a-form-item name="remotepeerlibvirturi">
              <template #label>
                <tooltip-label :title="$t('label.ftctl.remote.peer.libvirt.uri')" :tooltip="$t('placeholder.ftctl.remote.peer.libvirt.uri')" />
              </template>
              <a-input v-model:value="form.remotepeerlibvirturi" placeholder="qemu+ssh://root@10.0.0.12:22/system" />
            </a-form-item>
          </div>
        </div>
        <a-alert
          v-if="showSecondaryVmNameHint"
          class="ftctl-auto-fields"
          type="info"
          :showIcon="true"
          :message="$t('message.ftctl.secondary.vm.name.auto')"
          :description="secondaryVmNameDescription" />
        <a-form-item name="secondaryvmnameoverride">
          <a-checkbox v-model:checked="secondaryVmNameOverride" @change="handleSecondaryVmNameOverrideChange">
            {{ $t('label.ftctl.secondary.vm.name.override') }}
          </a-checkbox>
        </a-form-item>
        <a-form-item name="secondaryvmname">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.secondary.vm.name')" :tooltip="$t('placeholder.ftctl.secondary.vm.name')" />
          </template>
          <a-input v-model:value="form.secondaryvmname" :disabled="!secondaryVmNameOverride" :placeholder="$t('placeholder.ftctl.secondary.vm.name')" />
        </a-form-item>
        <a-form-item name="backendmode" v-if="showBackendFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.backend.mode')" :tooltip="$t('placeholder.ftctl.backend.mode')" />
          </template>
          <a-select v-model:value="form.backendmode" @change="handleBackendModeChange">
            <a-select-option value="shared-blockcopy" :disabled="requiresRemoteNbdBackend">shared-blockcopy</a-select-option>
            <a-select-option value="remote-nbd">remote-nbd</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="fencingpolicy" v-if="showBackendFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.fencing.policy')" :tooltip="$t('placeholder.ftctl.fencing.policy')" />
          </template>
          <a-select v-model:value="form.fencingpolicy">
            <a-select-option value="manual-block">manual-block</a-select-option>
            <a-select-option value="peer-virsh-destroy">peer-virsh-destroy</a-select-option>
            <a-select-option value="ipmi">ipmi</a-select-option>
          </a-select>
        </a-form-item>
        <a-alert
          v-if="showIpmiFencingInfo"
          class="ftctl-auto-fields"
          :type="peerHostIpmiReady ? 'info' : 'warning'"
          :showIcon="true"
          :message="$t(peerHostIpmiReady ? 'message.ftctl.ipmi.oobm.ready' : 'message.ftctl.ipmi.oobm.missing')"
          :description="ipmiFencingDescription" />
        <a-form-item name="secondarytargetdir" v-if="showRemoteNbdFields">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.secondary.target.dir')" :tooltip="$t('placeholder.ftctl.secondary.target.dir')" />
          </template>
          <a-input v-model:value="form.secondarytargetdir" placeholder="/secondary/ftctl/<vm>" />
        </a-form-item>
        <a-form-item name="remotenbdexportaddr" v-if="showRemoteNbdExportAddressField">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.remote.nbd.export.address')" :tooltip="$t('placeholder.ftctl.remote.nbd.export.address')" />
          </template>
          <a-input v-model:value="form.remotenbdexportaddr" :disabled="remoteNbdExportAddressReadOnly" placeholder="10.0.0.12:10809" />
        </a-form-item>
        <a-alert
          v-if="showFtFields"
          class="ftctl-auto-fields"
          type="info"
          :showIcon="true"
          :message="$t('message.ftctl.ft.storage.auto')"
          :description="ftEndpointSummary" />
        <a-alert
          v-if="showFtFields && ftMachineCompatibilityMessage"
          class="ftctl-auto-fields"
          :type="ftMachineCompatible ? 'info' : 'warning'"
          :showIcon="true"
          :message="$t(ftMachineCompatible ? 'message.ftctl.ft.machine.compatible' : 'message.ftctl.ft.machine.incompatible')"
          :description="ftMachineCompatibilityMessage" />
        <a-form-item v-if="showFtFields" name="manualxcoloendpoints">
          <a-checkbox v-model:checked="manualXcoloEndpoints" @change="handleManualXcoloEndpointsChange">
            {{ $t('label.ftctl.xcolo.manual.endpoints') }}
          </a-checkbox>
        </a-form-item>
        <a-form-item name="xcoloproxyendpoint" v-if="showFtFields && manualXcoloEndpoints">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.xcolo.proxy.endpoint')" :tooltip="$t('placeholder.ftctl.xcolo.proxy.endpoint')" />
          </template>
          <a-input v-model:value="form.xcoloproxyendpoint" placeholder="tcp:10.10.10.21:9000" />
        </a-form-item>
        <a-form-item name="xcolonbdendpoint" v-if="showFtFields && manualXcoloEndpoints">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.xcolo.nbd.endpoint')" :tooltip="$t('placeholder.ftctl.xcolo.nbd.endpoint')" />
          </template>
          <a-input v-model:value="form.xcolonbdendpoint" placeholder="tcp:10.10.20.21:10809" />
        </a-form-item>
        <a-form-item name="xcolomigrateuri" v-if="showFtFields && manualXcoloEndpoints">
          <template #label>
            <tooltip-label :title="$t('label.ftctl.xcolo.migrate.uri')" :tooltip="$t('placeholder.ftctl.xcolo.migrate.uri')" />
          </template>
          <a-input v-model:value="form.xcolomigrateuri" placeholder="tcp:10.10.20.21:9998" />
        </a-form-item>
      </a-form>
      <div class="action-button">
        <a-button @click="closeAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" :disabled="submitDisabled" type="primary" @click="handleSubmit" ref="submit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'RegisterFtctlProtection',
  components: {
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    protection: {
      type: Object,
      default: () => ({})
    }
  },
  data () {
    return {
      loading: false,
      hostsLoading: false,
      hosts: [],
      storagePoolsLoading: false,
      storagePools: [],
      networksLoading: false,
      networks: [],
      remoteMoldLoading: false,
      remoteMoldHostsLoading: false,
      remoteMoldStoragePoolsLoading: false,
      remoteMoldNetworksLoading: false,
      remoteMoldHosts: [],
      remoteMoldStoragePools: [],
      remoteMoldNetworks: [],
      remotePeerSshOverride: false,
      remotePeerSshAutoSetup: false,
      secondaryVmNameOverride: false,
      manualXcoloEndpoints: false
    }
  },
  computed: {
    vmRunningForProtection () {
      return String(this.resource?.state || '').toLowerCase() === 'running'
    },
    submitDisabled () {
      return !this.vmRunningForProtection || (this.showFtFields && this.hasFtMachineCompatibilityEvidence && !this.ftMachineCompatible)
    },
    hasFtMachineCompatibilityEvidence () {
      return this.protection && Object.prototype.hasOwnProperty.call(this.protection, 'ftmachinecompatible')
    },
    ftMachineCompatible () {
      return this.protection?.ftmachinecompatible === true || String(this.protection?.ftmachinecompatible).toLowerCase() === 'true'
    },
    ftMachineCompatibilityMessage () {
      if (!this.hasFtMachineCompatibilityEvidence) {
        return null
      }
      return this.protection?.ftmachinecompatibilitymessage || this.$t('message.ftctl.ft.machine.compatibility.unknown')
    },
    showFtFields () {
      return this.form?.mode === 'ft'
    },
    showBackendFields () {
      return this.form?.mode === 'ha' || this.form?.mode === 'dr'
    },
    showSecondaryVmNameHint () {
      return this.showBackendFields
    },
    secondaryVmNameDescription () {
      const target = this.resolveTargetSiteLabel()
      return this.$t('message.ftctl.secondary.vm.name.auto.desc', {
        name: this.form?.secondaryvmname || '-',
        target: target || '-'
      })
    },
    showDrPeerSiteType () {
      return this.form?.mode === 'dr'
    },
    isDrRemoteMold () {
      return this.form?.mode === 'dr' && this.form?.drpeersitetype === 'remote-mold'
    },
    showRemoteMoldFields () {
      return this.isDrRemoteMold
    },
    showLocalMoldPeerFields () {
      return !this.isDrRemoteMold
    },
    showStorageFields () {
      return this.form?.mode === 'ha' || this.form?.mode === 'dr' || this.form?.mode === 'ft'
    },
    showLocalStorageFields () {
      return this.showStorageFields && !this.isDrRemoteMold
    },
    showTargetNetworkFields () {
      return this.form?.mode === 'dr'
    },
    showLocalTargetNetworkFields () {
      return this.showTargetNetworkFields && !this.isDrRemoteMold
    },
    showRemoteTargetNetworkFields () {
      return this.showTargetNetworkFields && this.isDrRemoteMold
    },
    showRemoteNbdFields () {
      return this.showBackendFields && this.form?.backendmode === 'remote-nbd'
    },
    requiresRemoteNbdBackend () {
      return this.isDrRemoteMold || (this.showBackendFields && this.form?.targetstoragescope === 'secondary-local')
    },
    remoteNbdExportAddressReadOnly () {
      return this.isDrRemoteMold && !this.remotePeerSshOverride
    },
    showRemoteNbdExportAddressField () {
      return this.showRemoteNbdFields && (!this.showFtFields || this.manualXcoloEndpoints)
    },
    ftEndpointSummary () {
      if (!this.manualXcoloEndpoints) {
        return this.$t('message.ftctl.ft.endpoint.auto.pending')
      }
      if (!this.form?.peerhostid) {
        return this.$t('message.ftctl.ft.endpoint.auto.pending')
      }
      return [
        `${this.$t('label.ftctl.xcolo.proxy.endpoint')}: ${this.form.xcoloproxyendpoint || '-'}`,
        `${this.$t('label.ftctl.xcolo.nbd.endpoint')}: ${this.form.xcolonbdendpoint || '-'}`,
        `${this.$t('label.ftctl.xcolo.migrate.uri')}: ${this.form.xcolomigrateuri || '-'}`
      ].join(' / ')
    },
    selectedPeerHost () {
      return this.hosts.find(host => host.id === this.form?.peerhostid)
    },
    peerHostOobm () {
      return this.selectedPeerHost?.outofbandmanagement || {}
    },
    showIpmiFencingInfo () {
      return this.showBackendFields && this.form?.fencingpolicy === 'ipmi'
    },
    peerHostIpmiReady () {
      const oobm = this.peerHostOobm
      return !!this.form?.peerhostid &&
        String(oobm?.enabled) === 'true' &&
        String(oobm?.driver || '').toLowerCase() === 'ipmitool' &&
        !!oobm?.address &&
        !!oobm?.username
    },
    ipmiFencingDescription () {
      if (!this.form?.peerhostid) {
        return this.$t('message.ftctl.ipmi.oobm.select.peer')
      }
      const oobm = this.peerHostOobm
      return [
        `${this.$t('label.ftctl.ipmi.driver')}: ${oobm?.driver || '-'}`,
        `${this.$t('label.ftctl.ipmi.address')}: ${oobm?.address || '-'}`,
        `${this.$t('label.ftctl.ipmi.port')}: ${oobm?.port || '623'}`,
        `${this.$t('label.username')}: ${oobm?.username || '-'}`
      ].join(' / ')
    }
  },
  created () {
    this.initForm()
    this.fetchHosts()
    this.fetchStoragePools()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        mode: 'ha',
        drpeersitetype: 'local-mold',
        backendmode: 'shared-blockcopy',
        targetstoragescope: 'shared',
        targetstoragepoolid: null,
        networkids: [],
        fencingpolicy: 'manual-block',
        peerhostid: null,
        secondaryvmname: null,
        secondarytargetdir: null,
        remotenbdexportaddr: null,
        remotemoldapiurl: null,
        remotemoldapikey: null,
        remotemoldsecretkey: null,
        remotepeerhostuuid: null,
        remotepeerhostname: null,
        remotepeerhostaddress: null,
        remotepeerhostblockcopyaddress: null,
        remotepeersshuser: 'root',
        remotepeersshport: '22',
        remotepeersshautosetup: false,
        remotepeerlibvirturi: null,
        remotetargetstoragepooluuid: null,
        remotetargetstoragepoolname: null,
        remotetargetstoragepoolpath: null,
        remotetargetstoragepooltype: null,
        xcoloproxyendpoint: null,
        xcolonbdendpoint: null,
        xcolomigrateuri: null
      })
      this.rules = reactive({
        mode: [{ required: true, message: `${this.$t('label.required')}` }],
        targetstoragepoolid: [{ validator: this.validateLocalStorageRule, trigger: 'change' }],
        peerhostid: [{ validator: this.validateLocalPeerHostRule, trigger: 'change' }]
      })
      this.refreshSecondaryVmNameSuggestion(true)
    },
    validateLocalPeerHostRule () {
      if (this.showLocalMoldPeerFields && !this.form.peerhostid) {
        return Promise.reject(new Error(this.$t('label.required')))
      }
      return Promise.resolve()
    },
    validateLocalStorageRule () {
      if (this.showLocalStorageFields && !this.form.targetstoragepoolid) {
        return Promise.reject(new Error(this.$t('label.required')))
      }
      return Promise.resolve()
    },
    handleModeChange (mode) {
      if (mode === 'ft') {
        this.form.drpeersitetype = 'local-mold'
        this.form.backendmode = null
        this.form.secondarytargetdir = null
        this.form.remotenbdexportaddr = null
        this.form.networkids = []
        if (this.form.targetstoragepoolid) {
          this.applySelectedStoragePool(this.form.targetstoragepoolid)
        } else if (this.storagePools.length === 1) {
          this.form.targetstoragepoolid = this.storagePools[0].id
          this.applySelectedStoragePool(this.storagePools[0].id)
        }
        this.fetchStoragePools(this.form.peerhostid)
        this.applyPeerHostDefaults(this.form.peerhostid)
      } else {
        if (mode !== 'dr') {
          this.form.drpeersitetype = 'local-mold'
          this.form.networkids = []
          this.clearRemoteMoldSelection()
        }
        if (!this.form.backendmode) {
          this.form.backendmode = 'shared-blockcopy'
        }
        if (this.form.targetstoragepoolid) {
          this.applySelectedStoragePool(this.form.targetstoragepoolid)
        } else if (this.storagePools.length === 1) {
          this.form.targetstoragepoolid = this.storagePools[0].id
          this.applySelectedStoragePool(this.storagePools[0].id)
        }
        this.fetchStoragePools(this.form.peerhostid)
        if (mode === 'dr' && !this.isDrRemoteMold) {
          this.fetchTargetNetworks(this.form.peerhostid)
        }
        this.form.xcoloproxyendpoint = null
        this.form.xcolonbdendpoint = null
        this.form.xcolomigrateuri = null
      }
      this.refreshSecondaryVmNameSuggestion()
      this.fetchHosts()
    },
    handleDrPeerSiteTypeChange () {
      if (this.isDrRemoteMold) {
        this.form.backendmode = 'remote-nbd'
        this.form.targetstoragescope = 'secondary-local'
        this.form.peerhostid = null
        this.form.targetstoragepoolid = null
        this.form.networkids = []
        this.applyRemotePeerHostDefaults(this.form.remotepeerhostuuid)
      } else {
        this.clearRemoteMoldSelection()
        this.form.targetstoragescope = 'shared'
        if (!this.form.backendmode) {
          this.form.backendmode = 'shared-blockcopy'
        }
        this.fetchHosts()
        this.fetchStoragePools(this.form.peerhostid)
        this.fetchTargetNetworks(this.form.peerhostid)
      }
      this.refreshSecondaryVmNameSuggestion()
    },
    handleBackendModeChange (backendMode) {
      if (this.form.targetstoragepoolid) {
        this.applySelectedStoragePool(this.form.targetstoragepoolid)
      }
      if (this.requiresRemoteNbdBackend && backendMode !== 'remote-nbd') {
        this.form.backendmode = 'remote-nbd'
        this.applyPeerHostDefaults(this.form.peerhostid)
        return
      }
      if (backendMode === 'shared-blockcopy') {
        this.form.secondarytargetdir = null
        this.form.remotenbdexportaddr = null
      } else if (backendMode === 'remote-nbd') {
        this.applyPeerHostDefaults(this.form.peerhostid)
      }
    },
    fetchStoragePools (peerHostId = this.form?.peerhostid) {
      if (!this.resource?.zoneid || this.isDrRemoteMold) {
        return
      }
      this.storagePoolsLoading = true
      const peerHost = this.hosts.find(host => host.id === peerHostId)
      const targetZoneId = this.resolveLocalTargetZoneId(peerHost)
      const params = {
        zoneid: targetZoneId,
        listall: true,
        page: 1,
        pagesize: 500
      }
      if (peerHost?.id) {
        params.hostid = peerHost.id
      }
      if (peerHost?.clusterid) {
        params.clusterid = peerHost.clusterid
      } else if (this.resource.clusterid) {
        params.clusterid = this.resource.clusterid
      }
      getAPI('listStoragePools', params).then((json) => {
        const pools = json?.liststoragepoolsresponse?.storagepool || []
        this.storagePools = pools.filter(pool => !pool.state || pool.state === 'Up')
        const selectedPool = this.storagePools.find(pool => pool.id === this.form.targetstoragepoolid)
        if (selectedPool) {
          this.applySelectedStoragePool(selectedPool.id)
        } else if (this.storagePools.length === 1 && this.showStorageFields) {
          this.form.targetstoragepoolid = this.storagePools[0].id
          this.applySelectedStoragePool(this.storagePools[0].id)
        } else if (this.showStorageFields) {
          this.form.targetstoragepoolid = null
          this.form.targetstoragescope = 'shared'
        }
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.storagePoolsLoading = false
      })
    },
    fetchTargetNetworks (peerHostId = this.form?.peerhostid) {
      if (!this.showLocalTargetNetworkFields || !this.resource?.zoneid) {
        return
      }
      this.networksLoading = true
      const peerHost = this.hosts.find(host => host.id === peerHostId)
      const params = {
        zoneid: this.resolveLocalTargetZoneId(peerHost),
        listall: true,
        page: 1,
        pagesize: 500,
        canusefordeploy: true,
        traffictype: 'Guest',
        type: 'all',
        networkfilter: 'all'
      }
      getAPI('listNetworks', params).then((json) => {
        const networks = json?.listnetworksresponse?.network || []
        this.networks = networks
        this.applyNetworkSelectionDefaults(this.networks)
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.networksLoading = false
      })
    },
    resolveLocalTargetZoneId (peerHost) {
      if (this.form?.mode === 'dr' && peerHost?.zoneid) {
        return peerHost.zoneid
      }
      return this.resource.zoneid
    },
    handleStoragePoolChange (poolId) {
      this.applySelectedStoragePool(poolId)
    },
    applySelectedStoragePool (poolId) {
      const pool = this.storagePools.find(item => item.id === poolId)
      this.form.targetstoragescope = this.deriveTargetStorageScope(pool)
      if (this.requiresRemoteNbdBackend) {
        this.form.backendmode = 'remote-nbd'
        this.form.secondarytargetdir = this.deriveSecondaryTargetDir(pool)
        this.applyPeerHostDefaults(this.form.peerhostid)
      } else if (this.form.backendmode === 'shared-blockcopy') {
        this.form.secondarytargetdir = null
        this.form.remotenbdexportaddr = null
      }
    },
    deriveTargetStorageScope (pool) {
      if (!pool?.scope) {
        return 'shared'
      }
      const scope = String(pool.scope).toLowerCase()
      return scope === 'host' ? 'secondary-local' : scope
    },
    deriveSecondaryTargetDir (pool) {
      const path = pool?.path || pool?.url
      if (!path) {
        return this.form.secondarytargetdir
      }
      return String(path).replace(/\/+$/, '')
    },
    formatStoragePoolLabel (pool) {
      const name = pool?.name || pool?.path || pool?.id || '-'
      const scope = pool?.scope || '-'
      const type = pool?.type || pool?.storagetype || pool?.pooltype || '-'
      const cluster = pool?.clustername ? ` / ${pool.clustername}` : ''
      return `${name} (${scope}${cluster}, ${type})`
    },
    formatNetworkLabel (network) {
      const name = network?.name || network?.displaytext || network?.id || '-'
      const type = network?.type || network?.guesttype || '-'
      const zone = network?.zonename ? ` / ${network.zonename}` : ''
      const cidr = network?.cidr ? `, ${network.cidr}` : ''
      return network?.id ? `${name}${zone} (${type}${cidr}, ${network.id})` : `${name}${zone}`
    },
    applyNetworkSelectionDefaults (networks) {
      const current = Array.isArray(this.form.networkids) ? this.form.networkids : []
      const availableIds = new Set((networks || []).map(network => network.id))
      const stillValid = current.filter(id => availableIds.has(id))
      if (stillValid.length > 0) {
        this.form.networkids = stillValid
      } else if ((networks || []).length === 1 && this.showTargetNetworkFields) {
        this.form.networkids = [networks[0].id]
      } else if (this.showTargetNetworkFields) {
        this.form.networkids = []
      }
    },
    formatHostLabel (host) {
      const name = host?.name || host?.ipaddress || host?.id || '-'
      const migrationIp = host?.migrationip ? ` / ${host.migrationip}` : ''
      const zone = host?.zonename ? ` / ${host.zonename}` : ''
      return host?.id ? `${name}${migrationIp}${zone} (${host.id})` : `${name}${migrationIp}${zone}`
    },
    validateRemoteMoldConnection () {
      if (!this.validateRemoteMoldCredentials()) {
        return
      }
      this.remoteMoldLoading = true
      getAPI('validateFtctlRemoteMoldConnection', this.buildRemoteMoldCredentialParams()).then(() => {
        this.$message.success(this.$t('message.ftctl.remote.mold.connection.ok'))
        this.fetchRemoteMoldInventory()
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.remoteMoldLoading = false
      })
    },
    fetchRemoteMoldInventory () {
      if (!this.validateRemoteMoldCredentials()) {
        return
      }
      this.fetchRemoteMoldHosts()
      this.fetchRemoteMoldStoragePools()
      this.fetchRemoteMoldNetworks()
    },
    fetchRemoteMoldHosts () {
      this.remoteMoldHostsLoading = true
      getAPI('listFtctlRemoteMoldHosts', this.buildRemoteMoldCredentialParams()).then((json) => {
        const response = json?.listftctlremotemoldhostsresponse || {}
        const hosts = this.normalizeResponseItems(response, [
          'host',
          'hosts',
          'ftctlremotemoldhost',
          'ftctlremotemoldhosts'
        ])
        this.remoteMoldHosts = hosts
        if (hosts.length === 1) {
          this.handleRemotePeerHostChange(hosts[0].id)
        }
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.remoteMoldHostsLoading = false
      })
    },
    fetchRemoteMoldStoragePools () {
      this.remoteMoldStoragePoolsLoading = true
      const params = this.buildRemoteMoldCredentialParams()
      if (this.form.remotepeerhostuuid) {
        params.hostid = this.form.remotepeerhostuuid
      }
      getAPI('listFtctlRemoteMoldStoragePools', params).then((json) => {
        const response = json?.listftctlremotemoldstoragepoolsresponse || {}
        const pools = this.normalizeResponseItems(response, [
          'storagepool',
          'storagepools',
          'ftctlremotemoldstoragepool',
          'ftctlremotemoldstoragepools'
        ])
        this.remoteMoldStoragePools = pools
        if (pools.length === 1) {
          this.handleRemoteStoragePoolChange(pools[0].id)
        }
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.remoteMoldStoragePoolsLoading = false
      })
    },
    fetchRemoteMoldNetworks () {
      if (!this.validateRemoteMoldCredentials()) {
        return
      }
      this.remoteMoldNetworksLoading = true
      const params = this.buildRemoteMoldCredentialParams()
      const remotePeerHost = this.remoteMoldHosts.find(host => host.id === this.form.remotepeerhostuuid)
      if (remotePeerHost?.zoneid) {
        params.zoneid = remotePeerHost.zoneid
      }
      getAPI('listFtctlRemoteMoldNetworks', params).then((json) => {
        const response = json?.listftctlremotemoldnetworksresponse || {}
        const networks = this.normalizeResponseItems(response, [
          'network',
          'networks',
          'ftctlremotemoldnetwork',
          'ftctlremotemoldnetworks'
        ])
        this.remoteMoldNetworks = networks
        this.applyNetworkSelectionDefaults(this.remoteMoldNetworks)
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.remoteMoldNetworksLoading = false
      })
    },
    validateRemoteMoldCredentials () {
      if (!this.form.remotemoldapiurl || !this.form.remotemoldapikey || !this.form.remotemoldsecretkey) {
        this.$message.error(this.$t('message.ftctl.validation.remote.mold.credentials.required'))
        return false
      }
      return true
    },
    buildRemoteMoldCredentialParams () {
      return {
        remotemoldapiurl: this.form.remotemoldapiurl,
        remotemoldapikey: this.form.remotemoldapikey,
        remotemoldsecretkey: this.form.remotemoldsecretkey
      }
    },
    normalizeResponseItems (response, keys) {
      if (Array.isArray(response)) {
        return response
      }
      if (!response || typeof response !== 'object') {
        return []
      }
      for (const key of keys) {
        const value = response?.[key]
        if (Array.isArray(value)) {
          return value
        }
        if (value && typeof value === 'object') {
          const nestedItems = this.normalizeResponseItems(value, keys)
          if (nestedItems.length > 0) {
            return nestedItems
          }
          if (this.hasInventoryIdentity(value)) {
            return [value]
          }
        }
      }
      return []
    },
    hasInventoryIdentity (item) {
      return !!(item?.id || item?.name || item?.ipaddress || item?.path || item?.url)
    },
    filterRemoteInventoryOption (input, option) {
      const label = String(option?.label || option?.value || '')
      return label.toLowerCase().indexOf(String(input || '').toLowerCase()) >= 0
    },
    handleRemotePeerHostChange (hostId) {
      this.form.remotepeerhostuuid = hostId
      this.form.networkids = []
      this.applyRemotePeerHostDefaults(hostId)
      this.refreshSecondaryVmNameSuggestion()
      this.fetchRemoteMoldStoragePools()
      this.fetchRemoteMoldNetworks()
    },
    applyRemotePeerHostDefaults (hostId) {
      const peerHost = this.remoteMoldHosts.find(host => host.id === hostId)
      if (!peerHost) {
        return
      }
      const managementAddress = peerHost.ipaddress || peerHost.name
      const blockcopyAddress = peerHost.migrationip || peerHost.ipaddress || peerHost.name
      this.form.remotepeerhostname = peerHost.name || null
      this.form.remotepeerhostaddress = managementAddress
      this.form.remotepeerhostblockcopyaddress = blockcopyAddress
      if (!this.remotePeerSshOverride) {
        this.form.remotepeersshuser = 'root'
        this.form.remotepeersshport = '22'
        this.rebuildRemotePeerLibvirtUri()
        this.form.remotenbdexportaddr = this.buildHostPortEndpoint(blockcopyAddress, 10809)
      }
    },
    handleRemotePeerSshOverrideChange () {
      if (!this.remotePeerSshOverride) {
        this.applyRemotePeerHostDefaults(this.form.remotepeerhostuuid)
      } else {
        this.form.remotepeersshuser = this.form.remotepeersshuser || 'root'
        this.form.remotepeersshport = this.form.remotepeersshport || '22'
        this.rebuildRemotePeerLibvirtUri()
      }
    },
    rebuildRemotePeerLibvirtUri () {
      const host = this.form.remotepeerhostaddress
      if (!host) {
        this.form.remotepeerlibvirturi = null
        return
      }
      const user = this.form.remotepeersshuser || 'root'
      const port = this.form.remotepeersshport || '22'
      const authority = user ? `${user}@${host}` : host
      this.form.remotepeerlibvirturi = port ? `qemu+ssh://${authority}:${port}/system` : `qemu+ssh://${authority}/system`
    },
    handleRemoteStoragePoolChange (poolId) {
      const pool = this.remoteMoldStoragePools.find(item => item.id === poolId)
      this.form.remotetargetstoragepooluuid = poolId
      this.form.remotetargetstoragepoolname = pool?.name || null
      this.form.remotetargetstoragepoolpath = pool?.path || pool?.url || null
      this.form.remotetargetstoragepooltype = pool?.type || pool?.storagetype || pool?.pooltype || null
      this.form.targetstoragescope = 'secondary-local'
      this.form.backendmode = 'remote-nbd'
      this.form.secondarytargetdir = this.form.remotetargetstoragepoolpath
    },
    clearRemoteMoldSelection () {
      this.remoteMoldHosts = []
      this.remoteMoldStoragePools = []
      this.remoteMoldNetworks = []
      this.form.remotepeerhostuuid = null
      this.form.remotepeerhostname = null
      this.form.remotepeerhostaddress = null
      this.form.remotepeerhostblockcopyaddress = null
      this.form.remotepeersshuser = 'root'
      this.form.remotepeersshport = '22'
      this.form.remotepeerlibvirturi = null
      this.remotePeerSshOverride = false
      this.remotePeerSshAutoSetup = false
      this.form.remotetargetstoragepooluuid = null
      this.form.remotetargetstoragepoolname = null
      this.form.remotetargetstoragepoolpath = null
      this.form.remotetargetstoragepooltype = null
      this.form.networkids = []
      this.refreshSecondaryVmNameSuggestion()
    },
    fetchHosts () {
      this.hostsLoading = true
      const params = {
        type: 'Routing',
        state: 'Up',
        listall: true,
        details: 'all'
      }
      if (!(this.form?.mode === 'dr' && !this.isDrRemoteMold)) {
        params.zoneid = this.resource.zoneid
      }
      if (this.resource.clusterid && !(this.form?.mode === 'dr' && !this.isDrRemoteMold)) {
        params.clusterid = this.resource.clusterid
      }
      getAPI('listHosts', params).then((json) => {
        const allHosts = json?.listhostsresponse?.host || []
        this.hosts = allHosts.filter(host => host.hypervisor === 'KVM' && host.id !== this.resource.hostid)
        if (this.hosts.length === 1) {
          this.handlePeerHostChange(this.hosts[0].id)
        }
      }).catch((error) => {
        this.$notifyError(error)
      }).finally(() => {
        this.hostsLoading = false
      })
    },
    handlePeerHostChange (peerHostId) {
      this.form.peerhostid = peerHostId
      this.form.networkids = []
      this.applyPeerHostDefaults(peerHostId)
      this.refreshSecondaryVmNameSuggestion()
      if (this.showStorageFields) {
        this.fetchStoragePools(peerHostId)
      }
      if (this.showLocalTargetNetworkFields) {
        this.fetchTargetNetworks(peerHostId)
      }
    },
    handleManualXcoloEndpointsChange () {
      this.applyPeerHostDefaults(this.form.peerhostid)
    },
    applyPeerHostDefaults (peerHostId) {
      const peerHost = this.hosts.find(host => host.id === peerHostId)
      if (!peerHost) {
        return
      }
      const managementAddress = peerHost.ipaddress || peerHost.name
      const migrationAddress = peerHost.migrationip || peerHost.ipaddress || peerHost.name
      if (this.showFtFields) {
        if (this.manualXcoloEndpoints) {
          this.form.xcoloproxyendpoint = this.buildTcpEndpoint(managementAddress, 9000)
          this.form.xcolonbdendpoint = this.buildTcpEndpoint(migrationAddress, 10809)
          this.form.xcolomigrateuri = this.buildTcpEndpoint(migrationAddress, 9998)
        } else {
          this.form.xcoloproxyendpoint = null
          this.form.xcolonbdendpoint = null
          this.form.xcolomigrateuri = null
        }
      }
      if (this.showRemoteNbdExportAddressField) {
        this.form.remotenbdexportaddr = this.buildHostPortEndpoint(managementAddress, 10809)
      } else if (this.showFtFields) {
        this.form.remotenbdexportaddr = null
      }
    },
    buildTcpEndpoint (address, port) {
      return address ? `tcp:${address}:${port}` : null
    },
    buildHostPortEndpoint (address, port) {
      return address ? `${address}:${port}` : null
    },
    handleSecondaryVmNameOverrideChange () {
      if (!this.secondaryVmNameOverride) {
        this.refreshSecondaryVmNameSuggestion(true)
      }
    },
    refreshSecondaryVmNameSuggestion (force = false) {
      if (!this.form || (this.secondaryVmNameOverride && !force)) {
        return
      }
      this.form.secondaryvmname = this.buildSecondaryVmNameSuggestion()
    },
    buildSecondaryVmNameSuggestion () {
      const baseName = this.stripFtctlRoleSuffix(this.resource?.displayname || this.resource?.displayName || this.resource?.name || this.resource?.instancename || this.resource?.id || 'ftctl-vm')
      if (this.form?.mode === 'dr') {
        const suffix = this.resolveTargetSiteSuffix()
        return suffix ? `${baseName}-replica-${suffix}` : `${baseName}-replica`
      }
      return `${baseName}-standby`
    },
    stripFtctlRoleSuffix (name) {
      const normalized = String(name || '').trim()
      if (normalized.toLowerCase().endsWith('-standby')) {
        return normalized.slice(0, -8)
      }
      if (normalized.toLowerCase().endsWith('-replica')) {
        return normalized.slice(0, -8)
      }
      return normalized || 'ftctl-vm'
    },
    resolveTargetSiteLabel () {
      const host = this.isDrRemoteMold
        ? this.remoteMoldHosts.find(item => item.id === this.form?.remotepeerhostuuid)
        : this.hosts.find(item => item.id === this.form?.peerhostid)
      return host?.zonename || host?.clustername || host?.ipaddress || host?.name || null
    },
    resolveTargetSiteSuffix () {
      const host = this.isDrRemoteMold
        ? this.remoteMoldHosts.find(item => item.id === this.form?.remotepeerhostuuid)
        : this.hosts.find(item => item.id === this.form?.peerhostid)
      const address = host?.ipaddress || host?.name || host?.zonename || host?.clustername
      return this.sanitizeVmNameSegment(this.extractSiteToken(address))
    },
    extractSiteToken (value) {
      const normalized = String(value || '').trim()
      const ipParts = normalized.split('.')
      if (ipParts.length === 4 && /^[0-9]+$/.test(ipParts[2])) {
        return ipParts[2]
      }
      return normalized
    },
    sanitizeVmNameSegment (value) {
      const normalized = String(value || '')
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9-]+/g, '-')
        .replace(/^-+|-+$/g, '')
      return normalized || null
    },
    closeAction () {
      this.$emit('close-action')
    },
    validateConditionalFields (values) {
      if (this.isDrRemoteMold) {
        if (!values.remotemoldapiurl || !values.remotemoldapikey || !values.remotemoldsecretkey ||
            !values.remotepeerhostuuid || !values.remotepeerhostaddress || !values.remotepeerlibvirturi ||
            !values.remotetargetstoragepooluuid || !values.remotetargetstoragepoolpath) {
          this.$message.error(this.$t('message.ftctl.validation.remote.mold.required'))
          return false
        }
        if (values.remotepeersshport && !/^[0-9]+$/.test(String(values.remotepeersshport))) {
          this.$message.error(this.$t('message.ftctl.validation.remote.peer.ssh.port.required'))
          return false
        }
      }
      if (this.showTargetNetworkFields && (!Array.isArray(values.networkids) || values.networkids.length === 0)) {
        this.$message.error(this.$t('message.ftctl.validation.target.network.required'))
        return false
      }
      if (this.showBackendFields && !values.secondaryvmname) {
        this.$message.error(this.$t('message.ftctl.validation.secondary.vm.name.required'))
        return false
      }
      if (this.showRemoteNbdFields && (!values.secondarytargetdir || (this.showRemoteNbdExportAddressField && !values.remotenbdexportaddr))) {
        this.$message.error(this.$t('message.ftctl.validation.remote.nbd.required'))
        return false
      }
      if (this.showFtFields && this.manualXcoloEndpoints && (!values.xcoloproxyendpoint || !values.xcolonbdendpoint || !values.xcolomigrateuri)) {
        this.$message.error(this.$t('message.ftctl.validation.ft.required'))
        return false
      }
      if (this.showFtFields && this.hasFtMachineCompatibilityEvidence && !this.ftMachineCompatible) {
        this.$message.error(this.ftMachineCompatibilityMessage || this.$t('message.ftctl.ft.machine.incompatible'))
        return false
      }
      if (this.showLocalStorageFields && (!values.targetstoragepoolid || !values.targetstoragescope)) {
        this.$message.error(this.$t('message.ftctl.validation.target.storage.required'))
        return false
      }
      if (this.showIpmiFencingInfo && !this.peerHostIpmiReady) {
        this.$message.error(this.$t('message.ftctl.validation.ipmi.oobm.required'))
        return false
      }
      return true
    },
    handleSubmit (e) {
      if (e && e.preventDefault) {
        e.preventDefault()
      }
      if (this.loading) return
      if (!this.vmRunningForProtection) {
        this.$message.error(`FTCTL protection can be configured only when the VM is Running. Current VM state: ${this.resource?.state || '-'}`)
        return
      }
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        if (!this.validateConditionalFields(values)) {
          return
        }
        const params = {
          virtualmachineid: this.resource.id,
          mode: values.mode,
          provisioningbackend: 'cloud-managed',
          fencingpolicy: values.fencingpolicy
        }
        if (this.showDrPeerSiteType) {
          params.drpeersitetype = values.drpeersitetype || 'local-mold'
        }
        if (this.showBackendFields) {
          params.backendmode = values.backendmode
        }
        if (this.showLocalStorageFields) {
          params.targetstoragescope = values.targetstoragescope
          params.targetstoragepoolid = values.targetstoragepoolid
        }
        if (values.peerhostid && !this.isDrRemoteMold) {
          params.peerhostid = values.peerhostid
        }
        if (this.isDrRemoteMold) {
          Object.assign(params, {
            targetstoragescope: 'secondary-local',
            remotemoldapiurl: values.remotemoldapiurl,
            remotemoldapikey: values.remotemoldapikey,
            remotemoldsecretkey: values.remotemoldsecretkey,
            remotepeerhostuuid: values.remotepeerhostuuid,
            remotepeerhostname: values.remotepeerhostname,
            remotepeerhostaddress: values.remotepeerhostaddress,
            remotepeerhostblockcopyaddress: values.remotepeerhostblockcopyaddress,
            remotepeersshuser: values.remotepeersshuser,
            remotepeersshport: values.remotepeersshport,
            remotepeersshoverride: this.remotePeerSshOverride,
            remotepeersshautosetup: this.remotePeerSshAutoSetup,
            remotepeerlibvirturi: values.remotepeerlibvirturi,
            remotetargetstoragepooluuid: values.remotetargetstoragepooluuid,
            remotetargetstoragepoolname: values.remotetargetstoragepoolname,
            remotetargetstoragepoolpath: values.remotetargetstoragepoolpath,
            remotetargetstoragepooltype: values.remotetargetstoragepooltype
          })
        }
        if (values.secondaryvmname) {
          params.secondaryvmname = values.secondaryvmname
        }
        if (Array.isArray(values.networkids) && values.networkids.length > 0) {
          params.networkids = values.networkids.join(',')
        }
        if (this.showRemoteNbdFields) {
          params.secondarytargetdir = values.secondarytargetdir
          if (this.showRemoteNbdExportAddressField) {
            params.remotenbdexportaddr = values.remotenbdexportaddr
          }
        }
        if (this.showFtFields) {
          params.xcoloportallocationmode = this.manualXcoloEndpoints ? 'manual' : 'auto'
          if (this.manualXcoloEndpoints) {
            params.xcoloproxyendpoint = values.xcoloproxyendpoint
            params.xcolonbdendpoint = values.xcolonbdendpoint
            params.xcolomigrateuri = values.xcolomigrateuri
          }
        }
        this.loading = true
        const prepareRemoteSshAccess = () => {
          if (!this.isDrRemoteMold || !this.remotePeerSshAutoSetup) {
            return Promise.resolve()
          }
          return getAPI('prepareFtctlDrRemoteSshAccess', {
            virtualmachineid: this.resource.id,
            remotemoldapiurl: values.remotemoldapiurl,
            remotemoldapikey: values.remotemoldapikey,
            remotemoldsecretkey: values.remotemoldsecretkey,
            remotepeerhostuuid: values.remotepeerhostuuid,
            remotepeerhostaddress: values.remotepeerhostaddress,
            remotepeersshuser: values.remotepeersshuser,
            remotepeersshport: values.remotepeersshport,
            remotepeerlibvirturi: values.remotepeerlibvirturi,
            secondarytargetdir: values.secondarytargetdir,
            remotenbdexportaddr: values.remotenbdexportaddr
          })
        }
        prepareRemoteSshAccess().then(() => postAPI('registerFtctlProtection', params)).then((json) => {
          const payload = this.extractRegisterPayload(json)
          const jobId = this.extractJobId(payload)
          if (jobId) {
            this.$message.success(`${this.$t('message.ftctl.protection.saved')} (${this.$t('label.started')})`)
            this.pollRegisterJob(jobId, payload)
          } else {
            this.$message.success(this.$t('message.ftctl.protection.saved'))
          }
          this.$emit('refresh-data', payload)
          this.closeAction()
        }).catch((error) => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    extractRegisterPayload (response) {
      return response?.registerftctlprotectionresponse || response || {}
    },
    extractJobId (payload) {
      return payload?.jobid || payload?.jobId || null
    },
    pollRegisterJob (jobId, payload) {
      const refresh = (result) => {
        this.$emit('refresh-data', result?.jobresult || payload)
      }
      this.$pollJob({
        jobId,
        title: this.$t('label.ftctl.protection.configure'),
        description: this.resource?.name || this.resource?.displayname || this.resource?.id || '',
        resourceId: this.resource?.id,
        successMessage: this.$t('message.ftctl.protection.saved'),
        loadingMessage: `${this.$t('message.ftctl.protection.saved')} (${this.$t('label.started')})`,
        errorMessage: this.$t('label.error'),
        catchMessage: this.$t('label.error.caught'),
        successMethod: refresh,
        errorMethod: refresh
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.form-layout {
  width: 100%;
}

.ftctl-auto-fields {
  margin-bottom: 16px;

  :deep(.ant-alert-message) {
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-color);
  }

  :deep(.ant-alert-description) {
    font-size: 12px;
    line-height: 1.45;
    color: var(--text-color-secondary);
  }

  :deep(.ant-alert-icon) {
    font-size: 14px;
  }
}

.ftctl-remote-mold-fields {
  margin-bottom: 16px;
}

.ftctl-remote-mold-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: -4px 0 16px;
}

:global(.dark) .ftctl-auto-fields,
:global(.night) .ftctl-auto-fields,
:global([data-theme='dark']) .ftctl-auto-fields,
:global(body.dark-mode) .ftctl-auto-fields,
:global(body.dark) .ftctl-auto-fields,
:global(body.night) .ftctl-auto-fields {
  background: rgba(64, 169, 255, 0.1);
  border-color: rgba(64, 169, 255, 0.24);

  :deep(.ant-alert-message) {
    color: rgba(255, 255, 255, 0.82);
  }

  :deep(.ant-alert-description) {
    color: rgba(255, 255, 255, 0.58);
  }
}

:global(body.dark-mode) .ftctl-auto-fields.ant-alert-warning,
:global(body.dark) .ftctl-auto-fields.ant-alert-warning,
:global(body.night) .ftctl-auto-fields.ant-alert-warning,
:global(.dark) .ftctl-auto-fields.ant-alert-warning,
:global(.night) .ftctl-auto-fields.ant-alert-warning,
:global([data-theme='dark']) .ftctl-auto-fields.ant-alert-warning {
  background: rgba(250, 173, 20, 0.16);
  border-color: rgba(250, 173, 20, 0.46);

  :deep(.ant-alert-icon) {
    color: #ffc53d;
  }

  :deep(.ant-alert-message) {
    color: rgba(255, 255, 255, 0.9);
  }

  :deep(.ant-alert-description) {
    color: rgba(255, 255, 255, 0.68);
  }
}
</style>
