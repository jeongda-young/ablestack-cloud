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

export default {
  name: 'ssv1',
  title: 'label.shared.storage.vm',
  icon: 'DeploymentUnitOutlined',
  docHelp: 'adminguide/ssv.html',
  permission: ['listAdminSSV'],
  columns: ['name', 'state', 'displayname', 'sharedstoragevmtype', 'serviceofferingname', 'hostname', 'zonename'],
  details: ['name', 'id', 'state', 'sharedstoragevmtype', 'templatename', 'hostname', 'zonename', 'created', 'activeviewersessions', 'hostcontrolstate'],
  resourceType: 'AdminSSV',
  tabs: [{
    component: shallowRef(defineAsyncComponent(() => import('@/views/infra/SsvTabs.vue')))
  }],
  actions: [
    {
      api: 'startSSV',
      icon: 'caret-right-outlined',
      label: 'label.action.start.shared.storage.vm',
      message: 'message.action.start.shared.storage.vm',
      dataView: true,
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
      show: (record) => { return ['Stopped'].includes(record.state) }
    },
    {
      api: 'stopSSV',
      icon: 'poweroff-outlined',
      label: 'label.action.stop.shared.storage.vm',
      message: 'message.action.stop.shared.storage.vm',
      dataView: true,
      groupAction: true,
      popup: true,
      groupMap: (selection, values) => { return selection.map(x => { return { id: x, forced: values.forced } }) },
      show: (record) => { return ['Running'].includes(record.state) }
    },
    {
      api: 'deleteSSV',
      icon: 'delete-outlined',
      label: 'label.action.destroy.shared.storage.vm',
      message: 'message.action.destroy.shared.storage.vm',
      dataView: true,
      show: (record) => { return ['Running', 'Error', 'Stopped'].includes(record.state) },
      groupAction: true,
      popup: true,
      groupMap: (selection) => { return selection.map(x => { return { id: x } }) }
    }
  ]
}
