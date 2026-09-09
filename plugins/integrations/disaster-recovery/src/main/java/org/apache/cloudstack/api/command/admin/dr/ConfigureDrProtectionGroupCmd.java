// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.command.admin.dr;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.dr.DrPlanResponse;
import org.apache.cloudstack.api.response.dr.DrProtectionGroupResponse;

import com.cloud.dr.DrProtectionGroupService;

@APICommand(name = ConfigureDrProtectionGroupCmd.APINAME, description = "Configure a provider-neutral DR protection group",
        responseObject = DrProtectionGroupResponse.class, authorized = {RoleType.Admin})
public class ConfigureDrProtectionGroupCmd extends BaseCmd {
    public static final String APINAME = "configureDrProtectionGroup";
    @Inject private DrProtectionGroupService groupService;

    @Parameter(name = "planids", type = CommandType.LIST, collectionType = CommandType.UUID,
            entityType = DrPlanResponse.class, required = true, description = "ordered DR plan IDs")
    private List<Long> planIds;
    @Parameter(name = "groupname", type = CommandType.STRING, required = true, description = "protection group name")
    private String groupName;
    @Parameter(name = "maxparallel", type = CommandType.INTEGER, description = "maximum concurrent plans")
    private Integer maxParallel;
    @Parameter(name = "quiescerequired", type = CommandType.BOOLEAN, description = "require application quiesce policies")
    private Boolean quiesceRequired;

    @Override public void execute() {
        String uuid = groupService.configureGroup(planIds, groupName, maxParallel, quiesceRequired);
        DrProtectionGroupResponse response = new DrProtectionGroupResponse();
        response.setGroupUuid(uuid);
        response.setGroupName(groupName);
        response.setPlanCount(planIds != null ? planIds.size() : 0);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
    @Override public String getCommandName() { return APINAME.toLowerCase() + RESPONSE_SUFFIX; }
    @Override public long getEntityOwnerId() { return 0L; }
}
