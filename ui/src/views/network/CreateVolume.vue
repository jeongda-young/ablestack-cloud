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
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item ref="name" name="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.name"
          :placeholder="apiParams.name.description" />
      </a-form-item>
      <a-form-item ref="description" name="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="$t('placeholder.description')"/>
        </template>
        <a-input
          v-model:value="form.description"
          :placeholder="$t('placeholder.description')"/>
      </a-form-item>
      <a-form-item ref="zoneid" name="zoneid" v-if="!createVolumeFromSnapshot">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')" :tooltip="apiParams.zoneid.description"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          :loading="loading"
          @change="zone => fetchDiskOfferings(zone)"
          :placeholder="apiParams.zoneid.description"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-row :gutter="12" v-if="form.controlleruploadtype =='template'">
        <a-col :md="24" :lg="24">
          <a-form-item name="dctemplate" ref="dctemplate">
            <template #label>
              <tooltip-label :title="$t('label.dctemplate')" :tooltip="$t('placeholder.template')"/>
            </template>
            <a-select
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              v-model:value="form.dctemplate"
              :loading="template.loading"
              :placeholder="$t('placeholder.template')">
              <a-select-option v-for="opt in template.opts" :key="opt.id">
                {{ opt.name || opt.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :md="24" :lg="24">
          <a-form-item name="workstemplate" ref="workstemplate">
            <template #label>
              <tooltip-label :title="$t('label.workstemplate')" :tooltip="$t('placeholder.template')"/>
            </template>
            <a-select
              showSearch
              optionFilterProp="label"
              :filterOption="(input, option) => {
                return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }"
              v-model:value="form.workstemplate"
              :loading="template.loading"
              :placeholder="$t('placeholder.template')">
              <a-select-option v-for="opt2 in template.opts" :key="opt2.id">
                {{ opt2.name || opt2.description }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item name="protocol" ref="protocol" v-if="form.scope === 'zone' || form.scope === 'cluster' || form.scope === 'host'">
        <template #label>
          <tooltip-label :title="$t('label.protocol')" :tooltip="$t('message.protocol.description')"/>
        </template>
        <a-select
          v-model:value="form.protocol"
          @change="updateProtocolGlue"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :placeholder="apiParams.clusterid.description">
          <a-select-option :value="protocol" v-for="(protocol,idx) in protocols" :key="idx">
            {{ protocol }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <div
        v-if="form.protocol === 'nfs' || form.protocol === 'SMB' || form.protocol === 'iscsi' || form.protocol === 'vmfs'|| form.protocol === 'Gluster' || form.protocol === 'Linstor' ||
          (form.protocol === 'PreSetup' && hypervisorType === 'VMware') || form.protocol === 'datastorecluster' && form.protocol !== 'Glue'">
        <a-form-item name="server" ref="server">
          <template #label>
            <tooltip-label :title="$t('label.server')" :tooltip="$t('message.server.description')"/>
          </template>
          <a-input v-model:value="form.server" :placeholder="$t('message.server.description')" />
        </a-form-item>
      </div>
      <div v-if="form.protocol === 'nfs' || form.protocol === 'SMB' || form.protocol === 'ocfs2' || (form.protocol === 'PreSetup' && hypervisorType !== 'VMware') || form.protocol === 'SharedMountPoint'">
        <a-form-item name="path" ref="path">
          <template #label>
            <tooltip-label :title="$t('label.path')" :tooltip="$t('message.path.description')"/>
          </template>
          <a-input v-model:value="form.path" :placeholder="$t('message.path.description')"/>
        </a-form-item>
      </div>
      <div v-if="form.protocol === 'SMB'">
        <a-form-item :label="$t('label.smbusername')" name="smbUsername" ref="smbUsername">
          <a-input v-model:value="form.smbUsername"/>
        </a-form-item>
        <a-form-item :label="$t('label.smbpassword')" name="smbPassword" ref="smbPassword">
          <a-input-password v-model:value="form.smbPassword"/>
        </a-form-item>
        <a-form-item :label="$t('label.smbdomain')" name="smbDomain" ref="smbDomain">
          <a-input v-model:value="form.smbDomain"/>
        </a-form-item>
      </div>
      <div v-if="form.protocol === 'iscsi'">
        <a-form-item :label="$t('label.iqn')" name="iqn" ref="iqn">
          <a-input v-model:value="form.iqn"/>
        </a-form-item>
        <a-form-item :label="$t('label.lun')" name="lun" ref="lun">
          <a-input v-model:value="form.lun"/>
        </a-form-item>
      </div>
      <!--<div v-if="form.protocol === 'vmfs' || (form.protocol === 'PreSetup' && hypervisorType === 'VMware') || form.protocol === 'datastorecluster'">
        <a-form-item name="vCenterDataCenter" ref="vCenterDataCenter">
          <template #label>
            <tooltip-label :title="$t('label.vcenterdatacenter')" :tooltip="$t('message.datacenter.description')"/>
          </template>
          <a-input v-model:value="form.vCenterDataCenter" :placeholder="$t('message.datacenter.description')"/>
        </a-form-item>
        <a-form-item name="vCenterDataStore" ref="vCenterDataStore">
          <template #label>
            <tooltip-label :title="$t('label.vcenterdatastore')" :tooltip="$t('message.datastore.description')"/>
          </template>
          <a-input v-model:value="form.vCenterDataStore" :placeholder="$t('message.datastore.description')"/>
        </a-form-item>
      </div>
      <div v-if="form.protocol !== 'Linstor'">
        <a-form-item name="provider" ref="provider">
          <template #label>
            <tooltip-label :title="$t('label.providername')" :tooltip="apiParams.provider.description"/>
          </template>
          <a-select
            v-model:value="form.provider"
            @change="updateProviderAndProtocol"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }"
            :placeholder="apiParams.provider.description">
            <a-select-option :value="provider" v-for="(provider,idx) in providers" :key="idx">
              {{ provider }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </div>-->
      <a-form-item ref="networkid" name="networkid">
        <template #label>
          <tooltip-label :title="$t('label.networkid')" :tooltip="$t('placeholder.network')"/>
        </template>
        <a-select
          v-model:value="form.networkid"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :placeholder="$t('placeholder.network')"
          :loading="networkLoading"
          @change="val => { this.handleNetworkChange(this.networks[val]) }">
          >
          <a-select-option v-for="(opt, optIndex) in this.networks" :key="optIndex">
            <span v-if="opt.type!=='L2'">
              {{ opt.name || opt.description }} ({{ `${$t('label.cidr')}: ${opt.cidr}` }})
            </span>
            <span v-else>{{ opt.name || opt.description }}</span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="gateway" ref="gateway" class="form__item" :label="$t('label.reservedsystemgateway')">
        <a-input
          :placeholder="placeholder.gateway"
          v-model:value="form.gateway"
        />
      </a-form-item>

      <a-form-item name="netmask" ref="netmask" class="form__item" :label="$t('label.reservedsystemnetmask')">
        <a-input
          :placeholder="placeholder.netmask"
          v-model:value="form.netmask"
        />
      </a-form-item>
      <a-form-item ref="diskofferingid" name="diskofferingid" v-if="!createVolumeFromSnapshot || (createVolumeFromSnapshot && resource.volumetype === 'ROOT')">
        <template #label>
          <tooltip-label :title="$t('label.diskofferingid')" :tooltip="apiParams.diskofferingid.description || 'Disk Offering'"/>
        </template>
        <a-select
          v-model:value="form.diskofferingid"
          :loading="loading"
          @change="id => onChangeDiskOffering(id)"
          :placeholder="apiParams.diskofferingid.description || $t('label.diskofferingid')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(offering, index) in offerings"
            :value="offering.id"
            :key="index">
            {{ offering.displaytext || offering.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <span v-if="customDiskOffering">
        <a-form-item ref="size" name="size">
          <template #label>
            <tooltip-label :title="$t('label.sizegb')" :tooltip="apiParams.size.description"/>
          </template>
          <a-input
            v-model:value="form.size"
            :placeholder="apiParams.size.description"/>
        </a-form-item>
      </span>
      <span v-if="isCustomizedDiskIOps">
        <a-form-item ref="miniops" name="miniops">
          <template #label>
            <tooltip-label :title="$t('label.miniops')" :tooltip="apiParams.miniops.description"/>
          </template>
          <a-input
            v-model:value="form.miniops"
            :placeholder="apiParams.miniops.description"/>
        </a-form-item>
        <a-form-item ref="maxiops" name="maxiops">
          <template #label>
            <tooltip-label :title="$t('label.maxiops')" :tooltip="apiParams.maxiops.description"/>
          </template>
          <a-input
            v-model:value="form.maxiops"
            :placeholder="apiParams.maxiops.description"/>
        </a-form-item>
      </span>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateVolume',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      zones: [],
      offerings: [],
      customDiskOffering: false,
      loading: false,
      isCustomizedDiskIOps: false,
      placeholder: {
        name: null,
        gateway: null,
        netmask: null,
        startip: null,
        endip: null
      }
    }
  },
  computed: {
    createVolumeFromSnapshot () {
      return this.$route.path.startsWith('/snapshot')
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createVolume')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
        size: [{ required: true, message: this.$t('message.error.custom.disk.size') }],
        miniops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }],
        maxiops: [{
          validator: async (rule, value) => {
            if (value && (isNaN(value) || value <= 0)) {
              return Promise.reject(this.$t('message.error.number'))
            }
            return Promise.resolve()
          }
        }]
      })
      if (!this.createVolumeFromSnapshot) {
        this.rules.name = [{ required: true, message: this.$t('message.error.volume.name') }]
        this.rules.diskofferingid = [{ required: true, message: this.$t('message.error.select') }]
      }
    },
    fetchData () {
      this.loading = true
      api('listZones', { showicon: true }).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
        this.fetchDiskOfferings(this.form.zoneid)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchDiskOfferings (zoneId) {
      this.loading = true
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
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        if (this.createVolumeFromSnapshot) {
          values.snapshotid = this.resource.id
        }
        this.loading = true
        api('createVolume', values).then(response => {
          this.$pollJob({
            jobId: response.createvolumeresponse.jobid,
            title: this.$t('message.success.create.volume'),
            description: values.name,
            successMessage: this.$t('message.success.create.volume'),
            errorMessage: this.$t('message.create.volume.failed'),
            loadingMessage: this.$t('message.create.volume.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            gateway: values.gateway,
            netmask: values.netmask,
            startip: values.startip,
            endip: values.endip
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    },
    onChangeDiskOffering (id) {
      const offering = this.offerings.filter(x => x.id === id)
      this.customDiskOffering = offering[0]?.iscustomized || false
      this.isCustomizedDiskIOps = offering[0]?.iscustomizediops || false
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    width: 400px;
  }
}
</style>
