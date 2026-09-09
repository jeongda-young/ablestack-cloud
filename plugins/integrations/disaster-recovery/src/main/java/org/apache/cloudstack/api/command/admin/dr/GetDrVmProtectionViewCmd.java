// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
package org.apache.cloudstack.api.command.admin.dr;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.dr.DrVmProtectionViewResponse;

import com.cloud.dr.DrVmProtectionViewService;
import com.cloud.user.Account;

@APICommand(name = GetDrVmProtectionViewCmd.APINAME,
        description = "Get the local database DR plan view for a virtual machine",
        responseObject = DrVmProtectionViewResponse.class, authorized = {RoleType.Admin})
public class GetDrVmProtectionViewCmd extends BaseCmd {
    public static final String APINAME = "getDrVmProtectionView";

    @Inject private DrVmProtectionViewService drVmProtectionViewService;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID,
            entityType = UserVmResponse.class, required = true,
            description = "the virtual machine ID")
    private Long virtualMachineId;

    @Override
    public void execute() {
        DrVmProtectionViewResponse response = drVmProtectionViewService.getView(virtualMachineId);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override public String getCommandName() { return APINAME.toLowerCase() + RESPONSE_SUFFIX; }
    @Override public long getEntityOwnerId() { return Account.ACCOUNT_ID_SYSTEM; }
}
