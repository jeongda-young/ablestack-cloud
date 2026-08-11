// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package org.apache.cloudstack.api.command.user.vm;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuestNetworkRefreshResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.vm.guestnetwork.VmGuestNetworkApiService;

import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "refreshVirtualMachineGuestNetworkState",
        description = "Schedules a persisted guest network recollection without contacting the Agent in the API thread.",
        responseObject = GuestNetworkRefreshResponse.class,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.22.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RefreshVirtualMachineGuestNetworkStateCmd extends BaseCmd {
    @Inject
    private VmGuestNetworkApiService guestNetworkApiService;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID,
            entityType = UserVmResponse.class, required = true,
            description = "The ID of the Instance")
    private Long virtualMachineId;

    @Parameter(name = "sections", type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "Optional sections: interfaces,routes,dns,readiness")
    private List<String> sections;

    @Override
    public void execute() {
        Set<String> requested = sections == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(sections);
        GuestNetworkRefreshResponse response =
                guestNetworkApiService.requestRefresh(virtualMachineId, requested);
        response.setObjectName("guestnetworkrefresh");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _entityMgr.findById(UserVm.class, virtualMachineId);
        return vm == null ? Account.ACCOUNT_ID_SYSTEM : vm.getAccountId();
    }
}
