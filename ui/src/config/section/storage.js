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

import { shallowRef, defineAsyncComponent } from 'vue'
import store from '@/store'

export default {
  name: 'storage',
  title: 'label.storage',
  icon: 'database-outlined',
  children: [
    {
      name: 'volume',
      title: 'label.volumes',
      icon: 'hdd-outlined',
      docHelp: 'adminguide/storage.html#working-with-volumes',
      permission: ['listVolumesMetrics'],
      resourceType: 'Volume',
      filters: () => {
        if (store.getters.userInfo.roletype === 'Admin') {
          return ['user', 'all']
        } else {
          return []
        }
      },
      columns: () => {
        const fields = ['name', 'state', 'type', 'vmname', 'sizegb']
        const metricsFields = ['diskkbsread', 'diskkbswrite', 'diskiopstotal']

        if (store.getters.userInfo.roletype === 'Admin') {
          metricsFields.push({
            physicalsize: (record) => {
              return record.physicalsize ? parseFloat(record.physicalsize / (1024.0 * 1024.0 * 1024.0)).toFixed(2) + 'GB' : ''
            }
          })
        }
        metricsFields.push('utilization')

        if (store.getters.metrics) {
          fields.push(...metricsFields)
        }

        if (store.getters.userInfo.roletype === 'Admin') {
          fields.push('account')
          fields.push('storage')
        } else if (store.getters.userInfo.roletype === 'DomainAdmin') {
          fields.push('account')
        }
        fields.push('zonename')

        return fields
      },
      details: ['name', 'id', 'type', 'storagetype', 'diskofferingdisplaytext', 'deviceid', 'sizegb', 'physicalsize', 'provisioningtype', 'utilization', 'diskkbsread', 'diskkbswrite', 'diskioread', 'diskiowrite', 'diskiopstotal', 'miniops', 'maxiops', 'path'],
      related: [{
        name: 'snapshot',
        title: 'label.snapshots',
        param: 'volumeid'
      }],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'metrics',
          resourceType: 'Volume',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/StatsTab.vue'))),
          show: (record) => { return store.getters.features.instancesdisksstatsretentionenabled }
        },
        {
          name: 'events',
          resourceType: 'Volume',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/EventsTab.vue'))),
          show: () => { return 'listEvents' in store.getters.apis }
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      searchFilters: ['name', 'zoneid', 'domainid', 'account', 'state', 'tags'],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus-outlined',
          docHelp: 'adminguide/storage.html#creating-a-new-volume',
          label: 'label.action.create.volume',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateVolume.vue')))
        },
        {
          api: 'createVolume',
          icon: 'cloud-upload-outlined',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
          label: 'label.upload.volume.from.local',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UploadLocalVolume.vue')))
        },
        {
          api: 'uploadVolume',
          icon: 'link-outlined',
          docHelp: 'adminguide/storage.html#uploading-an-existing-volume-to-a-virtual-machine',
          label: 'label.upload.volume.from.url',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/UploadVolume.vue')))
        },
        {
          api: 'attachVolume',
          icon: 'paper-clip-outlined',
          label: 'label.action.attach.disk',
          dataView: true,
          show: (record) => { return ['Allocated', 'Ready', 'Uploaded'].includes(record.state) && !('virtualmachineid' in record) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/AttachVolume.vue')))
        },
        {
          api: 'detachVolume',
          icon: 'link-outlined',
          label: 'label.action.detach.disk',
          message: 'message.detach.disk',
          dataView: true,
          show: (record) => { return record.virtualmachineid && ['Running', 'Stopped', 'Destroyed'].includes(record.vmstate) }
        },
        {
          api: 'updateVolume',
          icon: 'edit-outlined',
          label: 'label.edit',
          dataView: true,
          args: ['name'],
          mapping: {
            account: {
              value: (record) => { return record.account }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        },
        {
          api: 'createSnapshot',
          icon: 'camera-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.take.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' && (record.hypervisor !== 'KVM' ||
              record.hypervisor === 'KVM' && record.vmstate === 'Running' && store.features.kvmsnapshotenabled ||
              record.hypervisor === 'KVM' && record.vmstate !== 'Running')
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/TakeSnapshot.vue')))
        },
        {
          api: 'createSnapshotPolicy',
          icon: 'clock-circle-outlined',
          docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
          label: 'label.action.recurring.snapshot',
          dataView: true,
          show: (record, store) => {
            return record.state === 'Ready' && (record.hypervisor !== 'KVM' ||
              record.hypervisor === 'KVM' && record.vmstate === 'Running' && store.features.kvmsnapshotenabled ||
              record.hypervisor === 'KVM' && record.vmstate !== 'Running')
          },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/RecurringSnapshotVolume.vue'))),
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            },
            intervaltype: {
              options: ['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY']
            }
          }
        },
        {
          api: 'resizeVolume',
          icon: 'fullscreen-outlined',
          docHelp: 'adminguide/storage.html#resizing-volumes',
          label: 'label.action.resize.volume',
          dataView: true,
          popup: true,
          show: (record) => { return ['Allocated', 'Ready'].includes(record.state) },
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ResizeVolume.vue')))
        },
        {
          api: 'migrateVolume',
          permission: ['migrateVolume', 'findStoragePoolsForMigration', 'listStoragePools', 'listDiskOfferings'],
          icon: 'drag-outlined',
          docHelp: 'adminguide/storage.html#id2',
          label: 'label.migrate.volume',
          args: ['volumeid', 'storageid', 'livemigrate'],
          dataView: true,
          show: (record, store) => { return record.state === 'Ready' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/MigrateVolume.vue')))
        },
        {
          api: 'changeOfferingForVolume',
          icon: 'swap-outlined',
          docHelp: 'adminguide/storage.html#id2',
          label: 'label.change.offering.for.volume',
          args: ['id', 'diskofferingid', 'size', 'miniops', 'maxiops', 'automigrate'],
          dataView: true,
          show: (record, store) => { return ['Allocated', 'Ready'].includes(record.state) && ['Admin'].includes(store.userInfo.roletype) },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/ChangeOfferingForVolume.vue')))
        },
        {
          api: 'extractVolume',
          icon: 'cloud-download-outlined',
          label: 'label.action.download.volume',
          message: 'message.download.volume.confirm',
          dataView: true,
          show: (record) => { return record.state === 'Ready' && (record.vmstate === 'Stopped' || !record.virtualmachineid) },
          args: ['zoneid', 'mode'],
          mapping: {
            zoneid: {
              value: (record) => { return record.zoneid }
            },
            mode: {
              value: (record) => { return 'HTTP_DOWNLOAD' }
            }
          },
          response: (result) => { return `Please click <a href="${result.volume.url}" target="_blank">${result.volume.url}</a> to download.` }
        },
        {
          api: 'createTemplate',
          icon: 'picture-outlined',
          label: 'label.action.create.template.from.volume',
          dataView: true,
          show: (record) => {
            return !['Destroy', 'Destroyed', 'Expunging', 'Expunged', 'Migrating', 'Uploading', 'UploadError', 'Creating'].includes(record.state) &&
            ((record.type === 'ROOT' && record.vmstate === 'Stopped') ||
            (record.type !== 'ROOT' && !record.virtualmachineid && !['Allocated', 'Uploaded'].includes(record.state)))
          },
          args: ['volumeid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled'],
          mapping: {
            volumeid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'recoverVolume',
          icon: 'medicine-box-outlined',
          label: 'label.action.recover.volume',
          message: 'message.action.recover.volume',
          dataView: true,
          show: (record, store) => {
            return (['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy'
          }
        },
        {
          api: 'deleteVolume',
          icon: 'delete-outlined',
          label: 'label.action.delete.volume',
          message: 'message.action.delete.volume',
          dataView: true,
          show: (record, store) => {
            return ['Expunging', 'Expunged', 'UploadError'].includes(record.state) ||
              ['Allocated', 'Uploaded'].includes(record.state) && record.type !== 'ROOT' && !record.virtualmachineid ||
              ((['Admin', 'DomainAdmin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume) && record.state === 'Destroy')
          },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'destroyVolume',
          icon: 'delete-outlined',
          label: 'label.action.destroy.volume',
          message: 'message.action.destroy.volume',
          dataView: true,
          args: (record, store) => {
            return (['Admin'].includes(store.userInfo.roletype) || store.features.allowuserexpungerecovervolume)
              ? ['expunge'] : []
          },
          show: (record, store) => {
            return !['Destroy', 'Destroyed', 'Expunging', 'Expunged', 'Migrating', 'Uploading', 'UploadError', 'Creating', 'Allocated', 'Uploaded'].includes(record.state) &&
              record.type !== 'ROOT' && !record.virtualmachineid
          }
        }
      ]
    },
    {
      name: 'snapshot',
      title: 'label.snapshots',
      icon: 'build-outlined',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listSnapshots'],
      resourceType: 'Snapshot',
      columns: () => {
        var fields = ['name', 'state', 'volumename', 'intervaltype', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('domain')
          fields.push('account')
        }
        return fields
      },
      details: ['name', 'id', 'volumename', 'intervaltype', 'account', 'domain', 'created'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      searchFilters: ['name', 'domainid', 'account', 'tags'],
      actions: [
        {
          api: 'createTemplate',
          icon: 'picture-outlined',
          label: 'label.create.template',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          args: ['snapshotid', 'name', 'displaytext', 'ostypeid', 'ispublic', 'isfeatured', 'isdynamicallyscalable', 'requireshvm', 'passwordenabled'],
          mapping: {
            snapshotid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'createVolume',
          icon: 'hdd-outlined',
          label: 'label.action.create.volume',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' },
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateVolume.vue')))
        },
        {
          api: 'revertSnapshot',
          icon: 'sync-outlined',
          label: 'label.action.revert.snapshot',
          message: 'message.action.revert.snapshot',
          dataView: true,
          show: (record) => { return record.state === 'BackedUp' && record.revertable }
        },
        {
          api: 'deleteSnapshot',
          icon: 'delete-outlined',
          label: 'label.action.delete.snapshot',
          message: 'message.action.delete.snapshot',
          dataView: true,
          show: (record) => { return record.state !== 'Destroyed' },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    },
    {
      name: 'vmsnapshot',
      title: 'label.vm.snapshots',
      icon: 'camera-outlined',
      docHelp: 'adminguide/storage.html#working-with-volume-snapshots',
      permission: ['listVMSnapshot'],
      resourceType: 'VMSnapshot',
      columns: () => {
        const fields = ['displayname', 'state', 'name', 'type', 'current', 'parentName', 'created']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('domain')
          fields.push('account')
        }
        return fields
      },
      details: ['name', 'id', 'displayname', 'description', 'type', 'current', 'parentName', 'virtualmachineid', 'account', 'domain', 'created'],
      searchFilters: ['name', 'domainid', 'account', 'tags'],
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'comments',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/AnnotationsTab.vue')))
        }
      ],
      actions: [
        {
          api: 'createSnapshotFromVMSnapshot',
          icon: 'camera-outlined',
          label: 'label.action.create.snapshot.from.vmsnapshot',
          message: 'message.action.create.snapshot.from.vmsnapshot',
          dataView: true,
          popup: true,
          show: (record) => { return (record.state === 'Ready' && record.hypervisor === 'KVM') },
          component: shallowRef(defineAsyncComponent(() => import('@/views/storage/CreateSnapshotFromVMSnapshot.vue')))
        },
        {
          api: 'revertToVMSnapshot',
          icon: 'sync-outlined',
          label: 'label.action.vmsnapshot.revert',
          message: 'label.action.vmsnapshot.revert',
          dataView: true,
          show: (record) => { return record.state === 'Ready' },
          args: ['vmsnapshotid'],
          mapping: {
            vmsnapshotid: {
              value: (record) => { return record.id }
            }
          }
        },
        {
          api: 'deleteVMSnapshot',
          icon: 'delete-outlined',
          label: 'label.action.vmsnapshot.delete',
          message: 'message.action.vmsnapshot.delete',
          dataView: true,
          show: (record) => { return ['Ready', 'Expunging', 'Error'].includes(record.state) },
          args: ['vmsnapshotid'],
          mapping: {
            vmsnapshotid: {
              value: (record) => { return record.id }
            }
          },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { vmsnapshotid: x } }) }
        }
      ]
    },
    {
      name: 'ssv',
      title: 'label.shared storage vm',
      icon: 'DeploymentUnitOutlined',
      docHelp: 'adminguide/ssv.html',
      permission: ['ListAdminSSV'],
      columns: ['name', 'state', 'networktype', 'publicip', 'serviceofferingname', 'hostname', 'zonename'],
      details: ['name', 'id', 'agentstate', 'publicip', 'privateip', 'gateway', 'hostname', 'zonename', 'created', 'activeviewersessions', 'hostcontrolstate'],
      resourceType: 'AdminSSV',
      tabs: [{
        component: shallowRef(defineAsyncComponent(() => import('@/views/compute/KubernetesServiceTab copy.vue')))
      }],
      actions: [
        {
          api: 'createVolume',
          icon: 'plus-outlined',
          docHelp: '',
          label: 'label.Create.SSV.VM',
          listView: true,
          popup: true,
          component: shallowRef(defineAsyncComponent(() => import('@/views/network/CreateVolume.vue')))
        },
        {
          api: 'startSystemVm',
          icon: 'caret-right-outlined',
          label: 'label.action.start.systemvm',
          message: 'message.action.start.systemvm',
          dataView: true,
          show: (record) => { return record.state === 'Stopped' },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'stopSystemVm',
          icon: 'poweroff-outlined',
          label: 'label.action.stop.systemvm',
          message: 'message.action.stop.systemvm',
          dataView: true,
          show: (record) => { return record.state === 'Running' },
          args: ['forced'],
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
        },
        {
          api: 'rebootSystemVm',
          icon: 'sync-outlined',
          label: 'label.action.reboot.systemvm',
          message: 'message.action.reboot.systemvm',
          dataView: true,
          show: (record) => { return record.state === 'Running' },
          args: ['forced'],
          groupAction: true,
          popup: true,
          groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) }
        },
        {
          api: 'scaleSystemVm',
          icon: 'arrows-alt-outlined',
          label: 'label.change.service.offering',
          message: 'message.confirm.scale.up.system.vm',
          dataView: true,
          show: (record) => { return record.state === 'Running' && record.hypervisor === 'VMware' || record.state === 'Stopped' },
          args: ['serviceofferingid'],
          mapping: {
            serviceofferingid: {
              api: 'listServiceOfferings',
              params: (record) => { return { virtualmachineid: record.id, issystem: true, systemvmtype: record.systemvmtype } }
            }
          }
        },
        {
          api: 'migrateSystemVm',
          icon: 'drag-outlined',
          label: 'label.action.migrate.systemvm',
          message: 'message.migrate.systemvm.confirm',
          dataView: true,
          show: (record, store) => { return record.state === 'Running' && ['Admin'].includes(store.userInfo.roletype) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateWizard'))),
          popup: true
        },
        {
          api: 'migrateSystemVm',
          icon: 'drag-outlined',
          label: 'label.action.migrate.systemvm.to.ps',
          dataView: true,
          show: (record, store) => { return ['Stopped'].includes(record.state) && ['VMware', 'KVM'].includes(record.hypervisor) },
          disabled: (record) => { return record.hostcontrolstate === 'Offline' },
          component: shallowRef(defineAsyncComponent(() => import('@/views/compute/MigrateVMStorage'))),
          popup: true
        },
        {
          api: 'runDiagnostics',
          icon: 'reconciliation-outlined',
          label: 'label.action.run.diagnostics',
          dataView: true,
          show: (record) => { return record.state === 'Running' },
          args: ['targetid', 'type', 'ipaddress', 'params'],
          mapping: {
            targetid: {
              value: (record) => { return record.id }
            },
            type: {
              options: ['ping', 'traceroute', 'arping']
            }
          },
          response: (result) => { return result && result.diagnostics ? `<strong>Output</strong>:<br/>${result.diagnostics.stdout}<br/><strong>Error</strong>: ${result.diagnostics.stderr}<br/><strong>Exit Code</strong>: ${result.diagnostics.exitcode}` : 'Invalid response' }
        },
        {
          api: 'getDiagnosticsData',
          icon: 'download-outlined',
          label: 'label.action.get.diagnostics',
          dataView: true,
          show: (record) => { return record.state === 'Running' },
          args: ['targetid', 'files'],
          mapping: {
            targetid: {
              value: (record) => { return record.id }
            }
          },
          response: (result) => { return result && result.diagnostics && result.diagnostics.url ? `Please click the link to download the retrieved diagnostics: <p><a href='${result.diagnostics.url}'>${result.diagnostics.url}</a></p>` : 'Invalid response' }
        },
        {
          api: 'patchSystemVm',
          icon: 'diff-outlined',
          label: 'label.action.patch.systemvm',
          message: 'message.action.patch.systemvm',
          dataView: true,
          show: (record) => { return ['Running'].includes(record.state) },
          args: ['forced'],
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        },
        {
          api: 'destroySystemVm',
          icon: 'delete-outlined',
          label: 'label.action.destroy.systemvm',
          message: 'message.action.destroy.systemvm',
          dataView: true,
          show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) },
          groupAction: true,
          popup: true,
          groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
        }
      ]
    }
  ]
}
