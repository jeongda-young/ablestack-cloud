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
package org.apache.cloudstack.api.command.admin.dr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrSiteHealthCheckResponse;
import org.apache.cloudstack.api.response.dr.DrSiteResponse;

import com.cloud.dr.DrSiteHealthCheckHistoryService;
import com.cloud.dr.DrSiteHealthCheckVO;
import com.cloud.dr.DrSiteService;
import com.cloud.dr.response.DrResponseGenerator;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;

@APICommand(name = ListDrSiteHealthChecksCmd.APINAME,
        description = "List Cross Hypervisor DR site health check history",
        responseObject = DrSiteHealthCheckResponse.class,
        authorized = {RoleType.Admin})
public class ListDrSiteHealthChecksCmd extends BaseListCmd {
    public static final String APINAME = "listDrSiteHealthChecks";

    @Inject
    private DrSiteService drSiteService;
    @Inject
    private DrSiteHealthCheckHistoryService drSiteHealthCheckHistoryService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "id", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true, description = "the DR site ID")
    private Long id;

    @Parameter(name = "healthstate", type = CommandType.STRING, description = "the health state filter")
    private String healthState;

    @Parameter(name = "triggertype", type = CommandType.STRING, description = "the health check trigger type filter")
    private String triggerType;

    @Parameter(name = "startdate", type = CommandType.DATE, description = "the health check start date")
    private Date startDate;

    @Parameter(name = "enddate", type = CommandType.DATE, description = "the health check end date")
    private Date endDate;

    @Override
    public void execute() {
        try {
            drSiteService.getSite(id);
            Filter filter = new Filter(DrSiteHealthCheckVO.class, "checkedAt", false, getStartIndex(), getPageSizeVal());
            Pair<List<DrSiteHealthCheckVO>, Integer> result = drSiteHealthCheckHistoryService.list(id, healthState, triggerType, startDate, endDate, filter);
            List<DrSiteHealthCheckResponse> responses = new ArrayList<DrSiteHealthCheckResponse>();
            for (DrSiteHealthCheckVO history : result.first()) {
                responses.add(drResponseGenerator.createSiteHealthCheckResponse(history));
            }
            ListResponse<DrSiteHealthCheckResponse> response = new ListResponse<DrSiteHealthCheckResponse>();
            response.setResponses(responses, result.second());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
