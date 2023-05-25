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
        <tooltip-label :title="$t('label.name')" :tooltip="$t('placeholder.name')"/>
      </template>
      <a-input
        v-model:value="form.name"
        :placeholder="$t('placeholder.name')"/>
    </a-form-item>
      <a-form-item ref="description" name="description">
        <template #label>
          <tooltip-label :title="$t('label.description')" :tooltip="$t('placeholder.description')"/>
        </template>
        <a-input
          v-model:value="form.description"
          :placeholder="$t('placeholder.description')"/>
      </a-form-item>
      <a-form-item ref="zoneid" name="zoneid">
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
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item ref="sharedstoragevmtype" name="sharedstoragevmtype">
            <template #label>
              <tooltip-label :title="$t('label.sharedstoragevmtype')" :tooltip="$t('placeholder.sharedstoragevmtype')"/>
            </template>
            <a-radio-group
              v-model:value="form.sharedstoragevmtype"
              buttonStyle="solid">
              <a-radio-button value="NFS">
                {{ $t('label.nfs') }}
              </a-radio-button>
              <a-radio-button value="SMB">
                {{ $t('label.smb') }}
              </a-radio-button>
              <a-radio-button value="ISCSI">
                {{ $t('label.iscsi') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          </a-col>
          </a-row>
      <a-form-item ref="diskofferingid" name="diskofferingid">
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
      <a-row :gutter="12">
        <a-col :md="24" :lg="24">
          <a-form-item ref="accessType" name="accessType">
            <template #label>
              <tooltip-label :title="$t('label.networktype')" :tooltip="$t('placeholder.accessType')"/>
            </template>
            <a-radio-group
              v-model:value="form.accessType"
              buttonStyle="solid"
              @change="selected => { handleAccessTypeChange(selected.target.value) }">
              <a-radio-button value="L2">
                {{ $t('label.L2') }}
              </a-radio-button>
              <a-radio-button value="Isolated">
                {{ $t('label.isolated') }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          </a-col>
          </a-row>
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
        <a-form-item ref="sharedstoragevmip"  name="sharedstoragevmip">
          <template #label>
            <tooltip-label :title="$t('label.sharedstoragevmip')" :tooltip="$t('placeholder.sharedstoragevmip')"/>
          </template>
          <a-input
            v-model:value="form.sharedstoragevmip"
            :placeholder="$t('placeholder.sharedstoragevmip')"/>
        </a-form-item>
      <a-form-item name="gateway" ref="gateway" class="form__item" :label="$t('label.systemgateway')">
        <a-input
          :placeholder="placeholder.gateway"
          v-model:value="form.gateway"
        />
      </a-form-item>
      <a-form-item name="netmask" ref="netmask" class="form__item" :label="$t('label.systemnetmask')">
        <a-input
          :placeholder="placeholder.netmask"
          v-model:value="form.netmask"
        />
      </a-form-item>
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
import store from '@/store'

export default {
  name: 'createSSV',
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
      sharedstoragevmtype: [],
      sharedstoragevmip: [],
      accessType: 'L2',
      networks: [],
      networkLoading: false,
      placeholder: {
        name: null,
        gateway: null,
        netmask: null
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createSSV')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        accessType: 'L2',
        sharedstoragevmtype: 'NFS',
        networkid: this.selectedNetwork
      })
      this.rules = reactive({
        name: [{ required: false, message: this.$t('message.error.required.input') }],
        description: [{ required: false, message: this.$t('message.error.required.input') }],
        sharedstoragevmtype: [{ required: false, message: this.$t('label.required') }],
        diskofferingid: [{ required: false, message: this.$t('message.error.select') }],
        gateway: [{ required: false, message: this.$t('label.required') }],
        netmask: [{ required: false, message: this.$t('label.required') }],
        networkid: [{ required: false, message: this.$t('message.error.select') }],
        zoneid: [{ required: false, message: this.$t('message.error.zone') }],
        sharedstoragevmip: [{ required: false, message: this.$t('message.error.required.input') }]
        // size: [{ required: true, message: this.$t('message.error.custom.disk.size') }],
      })
    },
    fetchDiskOfferings (zoneId) {
      this.loading = true
      api('listDiskOfferings', {
        zoneid: zoneId,
        listall: true
      }).then(json => {
        this.offerings = json.listdiskofferingsresponse.diskoffering || []
        this.customDiskOffering = this.offerings[0].iscustomized || false
        this.isCustomizedDiskIOps = this.offerings[0]?.iscustomizediops || false
      }).finally(() => {
        this.loading = false
      })
    },
    fetchData () {
      this.loading = true
      this.fetchNetworkData()
      api('listZones', { showicon: true }).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
        this.fetchDiskOfferings(this.form.zoneid)
      }).finally(() => {
        this.loading = false
      })
    },
    arrayHasItems (array) {
      return array !== null && array !== undefined && Array.isArray(array) && array.length > 0
    },
    handleAccessTypeChange (pvlan) {
      this.accessType = pvlan
      this.fetchNetworkData()
      // console.log(444444)
    },
    // fetchNetworkData () {
    //   const params = {}
    //   this.networkLoading = true
    //   api('listNetworks', params).then(json => {
    //     const listNetworks = json.listnetworksresponse.network
    //     if (this.arrayHasItems(listNetworks)) {
    //       this.networks = this.networks.concat(listNetworks)
    //     }
    //   }).finally(() => {
    //     this.networkLoading = false
    //     if (this.arrayHasItems(this.networks)) {
    //       this.form.networkid = 0
    //     }
    //   })
    // },
    // fetchL2Data () {
    //   this.networks = []
    //   const params = {
    //     domainid: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.domainid,
    //     account: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.account
    //   }
    //   this.networkLoading = true
    //   this.handleNetworkChange(null)
    //   api('listNetworks', params).then(json => {
    //     var items = json.listnetworksresponse.network
    //     if (items !== null) {
    //       if (this.accessType === 'L2') this.networks = items.filter(it => it.type.includes('L2'))
    //       this.handleNetworkChange(this.networks[0])
    //     }
    //   }).finally(() => {
    //     this.networkLoading = false
    //     if (this.arrayHasItems(this.networks)) {
    //       this.form.networkid = 0
    //     } else {
    //       this.form.networkid = null
    //     }
    //   })
    // },
    fetchNetworkData () {
      this.networks = []
      const params = {
        domainid: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.domainid,
        account: store.getters.project && store.getters.project.id ? null : store.getters.userInfo.account
      }
      this.networkLoading = true
      this.handleNetworkChange(null)
      api('listNetworks', params).then(json => {
        var items = json.listnetworksresponse.network
        if (items !== null) {
          if (this.accessType === 'L2') this.networks = items.filter(it => it.type.includes('L2'))
          if (this.accessType === 'Isolated') this.networks = items.filter(it => it.type.includes('Isolated'))
          this.handleNetworkChange(this.networks[0])
        }
      }).finally(() => {
        this.networkLoading = false
        console.log(this.networks)
        if (this.arrayHasItems(this.networks)) {
          this.form.networkid = 0
        } else {
          this.form.networkid = null
        }
      })
    },
    handleUploadTypeChange (val) {
      this.form.accessType = val
    },
    handleNetworkChange (network) {
      this.selectedNetwork = network
    },
    handleSubmit (e) {
      e.preventDefault()
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {
          name: values.name,
          description: values.description,
          diskofferingid: values.diskofferingid,
          networkid: this.selectedNetwork.id,
          gateway: values.gateway,
          netmask: values.netmask,
          sharedstoragevmtype: values.sharedstoragevmtype,
          sharedstoragevmip: values.sharedstoragevmip,
          zoneid: values.zoneid
        }
        if (values.zoneid === this.$t('label.all.zone')) {
          delete params.zoneid
        } else {
          params.zoneid = values.zoneid
        }
        // this.handleSubmit(params)
        this.loading = true
        api('createSSV', params).then(response => {
          this.$pollJob({
            jobId: response.createssvresponse.jobid,
            title: this.$t('message.success.create.ssv'),
            description: values.name,
            successMessage: this.$t('message.success.create.ssv'),
            errorMessage: this.$t('message.create.ssv.failed'),
            loadingMessage: this.$t('message.create.ssv.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
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
