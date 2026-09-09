// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.command.admin.dr;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrGroupRunResponse;

import com.cloud.dr.DrGroupRunVO;
import com.cloud.dr.DrProtectionGroupService;

@APICommand(name = ListDrProtectionGroupRunsCmd.APINAME, description = "List DR protection group execution history",
        responseObject = DrGroupRunResponse.class, authorized = {RoleType.Admin})
public class ListDrProtectionGroupRunsCmd extends BaseListCmd {
    public static final String APINAME = "listDrProtectionGroupRuns";
    @Inject private DrProtectionGroupService groupService;
    @Parameter(name = "groupuuid", type = CommandType.STRING, required = true, description = "protection group UUID")
    private String groupUuid;

    @Override public void execute() {
        List<DrGroupRunResponse> responses = new ArrayList<>();
        for (DrGroupRunVO run : groupService.listGroupRuns(groupUuid)) {
            DrGroupRunResponse response = new DrGroupRunResponse();
            response.setId(run.getUuid()); response.setGroupUuid(run.getGroupUuid()); response.setGroupName(run.getGroupName());
            response.setAction(run.getAction()); response.setState(run.getState()); response.setMaxParallel(run.getMaxParallel());
            response.setQuiesceRequired(run.isQuiesceRequired()); response.setTotalCount(run.getTotalCount());
            response.setSucceededCount(run.getSucceededCount()); response.setFailedCount(run.getFailedCount());
            response.setProgressJson(run.getProgressJson()); response.setCreated(run.getCreated()); response.setCompleted(run.getCompleted());
            response.setObjectName("drgrouprun"); responses.add(response);
        }
        ListResponse<DrGroupRunResponse> result = new ListResponse<>();
        result.setResponses(responses, responses.size()); result.setResponseName(getCommandName()); setResponseObject(result);
    }
    @Override public String getCommandName() { return APINAME.toLowerCase() + RESPONSE_SUFFIX; }
}
