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
package com.cloud.dr.cluster;

public class DisasterRecoveryClusterEventTypes {

    public static final String EVENT_DR_TEST_CONNECT = "DR.TEST.CONNECT";
    public static final String EVENT_DR_CREATE = "DR.CREATE";
    public static final String EVENT_DR_DELETE = "DR.DELETE";
    public static final String EVENT_DR_UPDATE = "DR.UPDATE";
    public static final String EVENT_DR_ENABLE = "DR.ENABLE";
    public static final String EVENT_DR_DISABLE = "DR.DISABLE";
    public static final String EVENT_DR_PROMOTE = "DR.PROMOTE";
    public static final String EVENT_DR_DEMOTE = "DR.DEMOTE";
    public static final String EVENT_DR_RESYNC = "DR.RESYNC";
    public static final String EVENT_DR_CLEAR = "DR.CLEAR";
    public static final String EVENT_DR_VM_CREATE = "DR.VM.CREATE";
    public static final String EVENT_DR_VM_UPDATE = "DR.VM.UPDATE";
    public static final String EVENT_DR_VM_DELETE = "DR.VM.DELETE";
    public static final String EVENT_DR_VM_START = "DR.VM.START";
    public static final String EVENT_DR_VM_STOP = "DR.VM.STOP";
    public static final String EVENT_DR_VM_PROMOTE = "DR.VM.PROMOTE";
    public static final String EVENT_DR_VM_DEMOTE = "DR.VM.DEMOTE";
    public static final String EVENT_DR_VM_SNAPSHOT = "DR.VM.SNAPSHOT";
    public static final String EVENT_DR_SITE_CREATE = "DR.SITE.CREATE";
    public static final String EVENT_DR_SITE_UPDATE = "DR.SITE.UPDATE";
    public static final String EVENT_DR_SITE_DELETE = "DR.SITE.DELETE";
    public static final String EVENT_DR_SITE_CHECK = "DR.SITE.CHECK";
    public static final String EVENT_DR_SITE_INVENTORY = "DR.SITE.INVENTORY";
    public static final String EVENT_DR_PLAN_INVENTORY = "DR.PLAN.INVENTORY";
    public static final String EVENT_DR_PLAN_SPEC_PREVIEW = "DR.PLAN.SPEC.PREVIEW";
    public static final String EVENT_DR_PLAN_CREATE = "DR.PLAN.CREATE";
    public static final String EVENT_DR_PLAN_UPDATE = "DR.PLAN.UPDATE";
    public static final String EVENT_DR_PLAN_DELETE = "DR.PLAN.DELETE";
    public static final String EVENT_DR_PLAN_SYNC = "DR.PLAN.SYNC";
    public static final String EVENT_DR_PLAN_PAUSE = "DR.PLAN.PAUSE";
    public static final String EVENT_DR_PLAN_RESUME = "DR.PLAN.RESUME";
    public static final String EVENT_DR_PLAN_TEST_FAILOVER = "DR.PLAN.TEST.FAILOVER";
    public static final String EVENT_DR_PLAN_FAILOVER = "DR.PLAN.FAILOVER";
    public static final String EVENT_DR_PLAN_FAILBACK = "DR.PLAN.FAILBACK";
    public static final String EVENT_DR_PLAN_REPROTECT = "DR.PLAN.REPROTECT";
    public static final String EVENT_DR_PLAN_RELEASE = "DR.PLAN.RELEASE";
    public static final String EVENT_DR_PLAN_PROJECTION_REFRESH = "DR.PLAN.PROJECTION.REFRESH";
    public static final String EVENT_DR_RUN_CANCEL = "DR.RUN.CANCEL";
    public static final String EVENT_DR_FENCE_CONFIRM = "DR.FENCE.CONFIRM";

}
