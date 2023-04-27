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
package com.cloud.ssv;

import java.util.ArrayList;
// import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.ssv.ListSSVCmd;
import org.apache.cloudstack.api.command.user.ssv.CreateSSVCmd;
import org.apache.cloudstack.api.command.user.ssv.DeleteSSVCmd;
import org.apache.cloudstack.api.command.user.ssv.StartSSVCmd;
import org.apache.cloudstack.api.command.user.ssv.StopSSVCmd;
import org.apache.cloudstack.api.command.admin.ssv.ListAdminSSVCmd;
import org.apache.cloudstack.api.response.SSVResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
// import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
// import org.apache.cloudstack.api.ApiConstants.VMDetails;
// import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.dc.DataCenterVO;
// import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.ssv.actionworkers.SSVDestroyWorker;
import com.cloud.ssv.actionworkers.SSVStartWorker;
import com.cloud.ssv.actionworkers.SSVStopWorker;
// import com.cloud.desktop.version.dao.DesktopTemplateMapDao;
import com.cloud.ssv.dao.SSVDao;
import com.cloud.ssv.dao.SSVVmMapDao;
// import com.cloud.desktop.version.DesktopTemplateMapVO;
// import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Network.GuestType;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.ServiceOfferingVO;
// import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.offering.ServiceOffering;
// import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
// import com.cloud.server.ResourceTag;
// import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.dao.ResourceTagDao;
// import com.cloud.event.ActionEvent;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
//import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.Ternary;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.component.ManagerBase;

import com.cloud.utils.fsm.NoTransitionException;
// import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
// import com.cloud.utils.db.Transaction;
// import com.cloud.utils.db.TransactionCallback;
// import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.component.ComponentContext;
// import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.exception.InvalidParameterValueException;

public class SSVManagerImpl extends ManagerBase implements SSVService {

    private static final Logger LOGGER = Logger.getLogger(SSVManagerImpl.class);

    protected StateMachine2<SSV.State, SSV.Event, SSV> _stateMachine = SSV.State.getStateMachine();

    ScheduledExecutorService _gcExecutor;
    ScheduledExecutorService _stateScanner;

    @Inject
    public SSVDao ssvDao;
    @Inject
    public SSVVmMapDao ssvVmMapDao;
    @Inject
    protected AccountManager accountManager;
    @Inject
    protected VMInstanceDao vmInstanceDao;
    @Inject
    protected AccountService accountService;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected UserVmJoinDao userVmJoinDao;
    @Inject
    protected IPAddressDao ipAddressDao;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected TemplateDataStoreDao _tmplStoreDao;
    @Inject
    protected NetworkService networkService;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected ResourceTagDao resourceTagDao;
    @Inject
    protected NetworkModel networkModel;

    private void logMessage(final Level logLevel, final String message, final Exception e) {
        if (logLevel == Level.WARN) {
            if (e != null) {
                LOGGER.warn(message, e);
            } else {
                LOGGER.warn(message);
            }
        } else {
            if (e != null) {
                LOGGER.error(message, e);
            } else {
                LOGGER.error(message);
            }
        }
    }

    private void logTransitStateAndThrow(final Level logLevel, final String message, final Long ssvId, final SSV.Event event, final Exception e) throws CloudRuntimeException {
        logMessage(logLevel, message, e);
        if (ssvId != null && event != null) {
            stateTransitTo(ssvId, event);
        }
        if (e == null) {
            throw new CloudRuntimeException(message);
        }
        throw new CloudRuntimeException(message, e);
    }

    private void logAndThrow(final Level logLevel, final String message) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, null);
    }

    private void logAndThrow(final Level logLevel, final String message, final Exception ex) throws CloudRuntimeException {
        logTransitStateAndThrow(logLevel, message, null, null, ex);
    }

    @Override
    public SSVResponse createSSVResponse(long ssvId) {
        SSVVO ssv = ssvDao.findById(ssvId);
        SSVResponse response = new SSVResponse();
        response.setObjectName(SSV.class.getSimpleName().toLowerCase());
        response.setId(ssv.getUuid());
        response.setName(ssv.getName());
        response.setDescription(ssv.getDescription());
        response.setState(ssv.getState().toString());
        response.setCreated(ssv.getCreated());

        DataCenterVO zone = ApiDBUtils.findZoneById(ssv.getZoneId());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());

        ServiceOfferingVO offering = serviceOfferingDao.findById(ssv.getServiceOfferingId());
        response.setServiceOfferingId(offering.getUuid());
        response.setServiceOfferingName(offering.getName());

        Account account = ApiDBUtils.findAccountById(ssv.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(account.getAccountName());
        }

        Domain domain = ApiDBUtils.findDomainById(ssv.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());

        NetworkVO ntwk = networkDao.findByIdIncludingRemoved(ssv.getNetworkId());
        response.setNetworkId(ntwk.getUuid());
        response.setAssociatedNetworkName(ntwk.getName());
        response.setNetworkType(ntwk.getGuestType());

        List<UserVmResponse> controlVmResponses = new ArrayList<UserVmResponse>();
        List<UserVmResponse> desktopVmResponses = new ArrayList<UserVmResponse>();
        List<SSVVmMapVO> controlVmList = ssvVmMapDao.listBySSVIdAndNotVmType(ssv.getId(), "desktopvm");

        ResponseView respView = ResponseView.Restricted;
        Account caller = CallContext.current().getCallingAccount();
        if (accountService.isRootAdmin(caller.getId())) {
            respView = ResponseView.Full;
        }
        // String responseName = "controlvmlist";
        // if (controlVmList != null && !controlVmList.isEmpty()) {
        //     for (SSVVmMapVO vmMapVO : controlVmList) {
        //         UserVmJoinVO userVM = userVmJoinDao.findById(vmMapVO.getVmId());
        //         if (userVM != null) {
        //             UserVmResponse cvmResponse = ApiDBUtils.newUserVmResponse(respView, responseName, userVM, EnumSet.of(VMDetails.nics), caller);
        //             controlVmResponses.add(cvmResponse);
        //             if("worksvm".equals(vmMapVO.getType())) {
        //                 List<? extends IpAddress> addresses = networkModel.listPublicIpsAssignedToGuestNtwk(ntwk.getId(), true);
        //                 for (IpAddress address : addresses) {
        //                     if (address.isSourceNat()) {
        //                         response.setWorksVmIp(address.getAddress().addr());
        //                     }
        //                 }
        //             }
        //             if("dcvm".equals(vmMapVO.getType())) {
        //                 response.setDcVmIp(userVM.getIpAddress());
        //             }
        //         }
        //     }
        // }

        // List<VMInstanceVO> vmList = vmInstanceDao.listByZoneId(ssv.getZoneId());
        // responseName = "desktopvmlist";
        // String resourceKey = "ClusterName";
        // if (vmList != null && !vmList.isEmpty()) {
        //     for (VMInstanceVO vmVO : vmList) {
        //         ResourceTag desktopvm = resourceTagDao.findByKey(vmVO.getId(), ResourceObjectType.UserVm, resourceKey);
        //         if (desktopvm != null) {
        //             if (desktopvm.getValue().equals(ssv.getName())) {
        //                 UserVmJoinVO userVM = userVmJoinDao.findById(vmVO.getId());
        //                 if (userVM != null) {
        //                     UserVmResponse dvmResponse = ApiDBUtils.newUserVmResponse(respView, responseName, userVM, EnumSet.of(VMDetails.nics), caller);
        //                     desktopVmResponses.add(dvmResponse);
        //                 }
        //             }
        //         }
        //     }
        // }

        // response.setControlVms(controlVmResponses);
        // response.setDesktopVms(desktopVmResponses);
        return response;
    }

    @Override
    public ListResponse<SSVResponse> listAdminSSV(ListAdminSSVCmd cmd) {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long ssvId = cmd.getId();
        final String state = cmd.getState();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();
        List<SSVResponse> responsesList = new ArrayList<SSVResponse>();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, ssvId, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(SSVVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SSVVO> sb = ssvDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.IN);
        SearchCriteria<SSVVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        if (state != null) {
            sc.setParameters("state", state);
        }
        if (keyword != null){
            sc.addOr("uuid", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (ssvId != null) {
            sc.setParameters("id", ssvId);
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        List<SSVVO> desktop = ssvDao.search(sc, searchFilter);
        for (SSVVO cluster : desktop) {
            SSVResponse ssvResponse = createSSVResponse(cluster.getId());
            responsesList.add(ssvResponse);
        }
        ListResponse<SSVResponse> response = new ListResponse<SSVResponse>();
        response.setResponses(responsesList);
        return response;
    }

    @Override
    public ListResponse<SSVResponse> listSSV(ListSSVCmd cmd) {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final Long ssvId = cmd.getId();
        final String state = cmd.getState();
        final String name = cmd.getName();
        final String keyword = cmd.getKeyword();
        List<SSVResponse> responsesList = new ArrayList<SSVResponse>();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, Project.ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, ssvId, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(SSVVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<SSVVO> sb = ssvDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.IN);
        SearchCriteria<SSVVO> sc = sb.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        if (state != null) {
            sc.setParameters("state", state);
        }
        if (keyword != null){
            sc.addOr("uuid", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (ssvId != null) {
            sc.setParameters("id", ssvId);
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        List<SSVVO> desktop = ssvDao.search(sc, searchFilter);
        for (SSVVO cluster : desktop) {
            SSVResponse ssvResponse = createSSVResponse(cluster.getId());
            responsesList.add(ssvResponse);
        }
        ListResponse<SSVResponse> response = new ListResponse<SSVResponse>();
        response.setResponses(responsesList);
        return response;
    }


    protected boolean stateTransitTo(long ssvId, SSV.Event e) {
        SSVVO ssv = ssvDao.findById(ssvId);
        try {
            return _stateMachine.transitTo(ssv, e, null, ssvDao);
        } catch (NoTransitionException nte) {
            LOGGER.warn(String.format("Failed to transition state of the Desktop cluster : %s in state %s on event %s", ssv.getName(), ssv.getState().toString(), e.toString()), nte);
            return false;
        }
    }

    @Override
    public SSV createSSV(CreateSSVCmd cmd) throws CloudRuntimeException {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }

        validateSSVCreateParameters(cmd);

        final String L2Type = "internal";
        final ServiceOffering serviceOffering = serviceOfferingDao.findById(cmd.getServiceOfferingId());
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        // final SSVVO cluster = Transaction.execute(new TransactionCallback<SSVVO>() {
            // @Override
            // public SSVVO doInTransaction(TransactionStatus status) {
                // SSVVO newCluster = new SSVVO(cmd.getName(), cmd.getDescription(), clusterSSV.getZoneId(), clusterSSV.getId(),
                //         serviceOffering.getId(), cmd.getAdDomainName(), cmd.getNetworkId(), cmd.getAccessType(),
                //         owner.getDomainId(), owner.getAccountId(), SSV.State.Created, cmd.getDcIp(), cmd.getWorksIp());
                // ssvDao.persist(newCluster);
                // if(cmd.getAccessType().equals(L2Type)){
                //     addSSVIpRangeInDeployCluster(newCluster, cmd);
                // }
            //     return newCluster;
            // }
        // });
        // if (LOGGER.isInfoEnabled()) {
        //     LOGGER.info(String.format("Desktop cluster name: %s and ID: %s has been created", cluster.getName(), cluster.getUuid()));
        // }
        // return cluster;
        return null;
    }

    private void validateSSVCreateParameters(final CreateSSVCmd cmd) throws CloudRuntimeException {
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final String adDomainName = cmd.getAdDomainName();
        // final String password = cmd.getPassword();
        // final Long ssvId = cmd.getControllerVersion();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Long networkId = cmd.getNetworkId();

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterValueException("Invalid name for the Desktop cluster name:" + name);
        }
        if (!NetUtils.verifyDomainNameLabel(name, true) || name.length() > 8) {
            throw new InvalidParameterValueException("Invalid name. desktop cluster name can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 8 characters long, and can't start or end with \"-\" and can't start with digit");
        }
        final List<SSVVO> clusters = ssvDao.listAll();
        for (final SSVVO cluster : clusters) {
            final String otherName = cluster.getName();
            final String otherAdDomainName = cluster.getAdDomainName();
            final Long otherNetwork = cluster.getNetworkId();
            if (otherName.equals(name)) {
                throw new InvalidParameterValueException("cluster name '" + name + "' already exists.");
            }
            if (otherAdDomainName.equals(adDomainName)) {
                throw new InvalidParameterValueException("cluster ad domain name '" + adDomainName + "' already exists.");
            }
            if (otherNetwork.equals(networkId)){
                throw new InvalidParameterValueException("cluster network id '" + networkId + "' already cluster deployed.");
            }
        }
        if (description == null || description.isEmpty()) {
            throw new InvalidParameterValueException("Invalid description for the Desktop cluster description:" + description);
        }

        // final DesktopControllerVersion clusterSSV = desktopControllerVersionDao.findById(ssvId);
        // if (clusterSSV == null) {
        //     throw new InvalidParameterValueException("Unable to find given Desktop version in supported versions");
        // }
        // if (!DesktopControllerVersion.State.Enabled.equals(clusterSSV.getState())) {
        //     throw new InvalidParameterValueException(String.format("Desktop version ID: %s is in %s state", clusterSSV.getUuid(), clusterSSV.getState()));
        // }

        // TemplateDataStoreVO tmpltStoreRef = null;
        // List<DesktopTemplateMapVO> templateList = desktopTemplateMapDao.listByVersionId(clusterSSV.getId());
        // if (templateList != null && !templateList.isEmpty()) {
        //     for (DesktopTemplateMapVO templateMapVO : templateList) {
        //         tmpltStoreRef = _tmplStoreDao.findByStoreTemplate(clusterSSV.getZoneId(), templateMapVO.getTemplateId());
        //         if (tmpltStoreRef != null) {
        //             if (tmpltStoreRef.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
        //                 throw new InvalidParameterValueException("Unable to deploy cluster, Desktop Controller Template " + templateMapVO.getTemplateId() + " has not been completely downloaded to zone " + clusterSSV.getZoneId());
        //             }
        //         }
        //     }
        // }

        // DataCenter zone = dataCenterDao.findById(clusterSSV.getZoneId());
        // if (zone == null) {
        //     throw new InvalidParameterValueException("Unable to find zone by ID: " + clusterSSV.getZoneId());
        // }
        // if (Grouping.AllocationState.Disabled == zone.getAllocationState()) {
        //     throw new PermissionDeniedException(String.format("Cannot perform this operation, zone ID: %s is currently disabled", zone.getUuid()));
        // }
        // if (clusterSSV.getZoneId() != null && !clusterSSV.getZoneId().equals(zone.getId())) {
        //     throw new InvalidParameterValueException(String.format("Desktop version ID: %s is not available for zone ID: %s", clusterSSV.getUuid(), zone.getUuid()));
        // }
        // if (clusterSSV.getZoneId() != null && clusterSSV.getZoneId() != zone.getId()) {
        //     throw new InvalidParameterValueException(String.format("Desktop version ID: %s is not available for zone ID: %s", clusterSSV.getUuid(), zone.getUuid()));
        // }

        ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("No service offering with ID: " + serviceOfferingId);
        }

        Network network = null;
        if (networkId != null) {
            network = networkService.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network with given ID");
            }
            final String dcIp = cmd.getDcIp();
            final String worksIp = cmd.getWorksIp();
            final String cider = network.getCidr();

            if (worksIp == null || worksIp.isEmpty()) {
                throw new InvalidParameterValueException("Invalid IP for the Desktop cluster Works VM IP:" + worksIp);
            }
            // if (network.getGuestType().equals(GuestType.L2)){
            //     //L2 일 경우 IP 범위 조회하여 벨리데이션 체크
            //     final String gateway = cmd.getGateway();
            //     final String netmask = cmd.getNetmask();
            //     final String startIp = cmd.getStartIp();
            //     final String endIp = cmd.getEndIp();

            //     if (gateway == null || gateway.isEmpty()) {
            //         throw new InvalidParameterValueException("Invalid gateway for the Desktop cluster gateway:" + gateway);
            //     }
            //     if (netmask == null || netmask.isEmpty()) {
            //         throw new InvalidParameterValueException("Invalid netmask for the Desktop cluster netmask:" + netmask);
            //     }
            //     if (startIp == null || startIp.isEmpty()) {
            //         throw new InvalidParameterValueException("Invalid startIp for the Desktop cluster nastartIpme:" + startIp);
            //     }
            //     if (endIp == null || endIp.isEmpty()) {
            //         throw new InvalidParameterValueException("Invalid endIp for the Desktop cluster endIp:" + endIp);
            //     }
            //     if (!NetUtils.isValidIp4(gateway)) {
            //         throw new InvalidParameterValueException("Please specify a valid gateway");
            //     }
            //     if (!NetUtils.isValidIp4Netmask(netmask)) {
            //         throw new InvalidParameterValueException("Please specify a valid netmask");
            //     }
            //     final String newCidr = NetUtils.getCidrFromGatewayAndNetmask(gateway, netmask);
            //     if (!NetUtils.isIpWithInCidrRange(gateway, newCidr) || !NetUtils.isIpWithInCidrRange(startIp, newCidr) || !NetUtils.isIpWithInCidrRange(endIp, newCidr)) {
            //         throw new InvalidParameterValueException("Please specify a valid IP range or valid netmask or valid gateway");
            //     }
            //     final List<SSVIpRangeVO> ips = ssvIpRangeDao.listAll();
            //     for (final SSVIpRangeVO range : ips) {
            //         final String otherGateway = range.getGateway();
            //         final String otherNetmask = range.getNetmask();
            //         final String otherStartIp = range.getStartIp();
            //         final String otherEndIp = range.getEndIp();
            //         if ( otherGateway == null || otherNetmask == null ) {
            //             continue;
            //         }
            //         final String otherCidr = NetUtils.getCidrFromGatewayAndNetmask(otherGateway, otherNetmask);
            //         if( !NetUtils.isNetworksOverlap(newCidr,  otherCidr)) {
            //             continue;
            //         }
            //         if (!gateway.equals(otherGateway) || !netmask.equals(range.getNetmask())) {
            //             throw new InvalidParameterValueException("The IP range has already been added with gateway "
            //                     + otherGateway + " ,and netmask " + otherNetmask
            //                     + ", Please specify the gateway/netmask if you want to extend ip range" );
            //         }
            //         if (!NetUtils.is31PrefixCidr(newCidr)) {
            //             if (NetUtils.ipRangesOverlap(startIp, endIp, otherStartIp, otherEndIp)) {
            //                 throw new InvalidParameterValueException("The IP range already has IPs that overlap with the new range." +
            //                         " Please specify a different start IP/end IP.");
            //             }
            //         }
            //     }
            //     //L2 일 경우 dc ip, works ip 입력된 경우 벨리데이션 체크
            //     if ((dcIp != null && !dcIp.isEmpty()) && (worksIp != null && !worksIp.isEmpty())) {
            //         if (!NetUtils.isIpInRange(dcIp, startIp, endIp) || !NetUtils.isIpInRange(worksIp, startIp, endIp)) {
            //             throw new InvalidParameterValueException("DC or Works VM IP provided is not within the specified range: " + startIp + " - " + endIp);
            //         }
            //         if (dcIp == worksIp) {
            //             throw new InvalidParameterValueException("Please enter different Works IP and DC IP");
            //         }
            //     }
            // }
            if (network.getGuestType().equals(GuestType.Isolated) || network.getGuestType().equals(GuestType.Shared)) {
                //Isolated, Shared 일 경우 dc ip, works ip 입력된 경우 벨리데이션 체크
                if ((dcIp != null && !dcIp.isEmpty()) && (worksIp != null && !worksIp.isEmpty())) {
                    if (!NetUtils.isIpWithInCidrRange(dcIp, cider) || !NetUtils.isIpWithInCidrRange(worksIp, cider)) {
                        throw new InvalidParameterValueException("Please specify a valid IP range or valid netmask or valid gateway");
                    }
                    if (dcIp == worksIp) {
                        throw new InvalidParameterValueException("Please enter different Works IP and DC IP");
                    }
                }
            }
        }
    }

    // private void addSSVIpRangeInDeployCluster(final SSV ssv, final CreateSSVCmd cmd) {
    //     final long ssvId = ssv.getId();
    //     final String gateway = cmd.getGateway();
    //     final String netmask = cmd.getNetmask();
    //     final String startIp = cmd.getStartIp();
    //     final String endIp = cmd.getEndIp();
    //     Transaction.execute(new TransactionCallbackNoReturn() {
    //         @Override
    //         public void doInTransactionWithoutResult(TransactionStatus status) {
    //             SSVIpRangeVO iprange = new SSVIpRangeVO(ssvId, gateway, netmask, startIp, endIp);
    //             ssvIpRangeDao.persist(iprange);
    //         }
    //     });
    // }

    @Override
    public boolean startSSV(long ssvId, boolean onCreate) throws CloudRuntimeException {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }
        final SSVVO ssv = ssvDao.findById(ssvId);
        if (ssv == null) {
            throw new InvalidParameterValueException("Failed to find Desktop cluster with given ID");
        }
        if (ssv.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Desktop cluster : %s is already deleted", ssv.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, ssv);
        if (ssv.getState().equals(SSV.State.Running)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Desktop cluster : %s is in running state", ssv.getName()));
            }
            return true;
        }
        if (ssv.getState().equals(SSV.State.Starting)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Desktop cluster : %s is already in starting state", ssv.getName()));
            }
            return true;
        }
        // final DesktopControllerVersion clusterSSV = desktopControllerVersionDao.findById(ssv.getSsvId());
        // final DataCenter zone = dataCenterDao.findById(clusterSSV.getZoneId());
        // if (zone == null) {
        //     logAndThrow(Level.WARN, String.format("Unable to find zone for Desktop cluster : %s", ssv.getName()));
        // }
        SSVStartWorker startWorker = new SSVStartWorker(ssv, this);
        startWorker = ComponentContext.inject(startWorker);
        if (onCreate) {
            // Start for Desktop cluster in 'Created' state
            return startWorker.startSSVOnCreate();
        } else {
            // Start for Desktop cluster in 'Stopped' state. Resources are already provisioned, just need to be started
            return startWorker.startStoppedSSV();
        }
    }

    @Override
    public boolean stopSSV(long ssvId) throws CloudRuntimeException {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }
        final SSVVO ssv = ssvDao.findById(ssvId);
        if (ssv == null) {
            throw new InvalidParameterValueException("Failed to find Desktop cluster with given ID");
        }
        if (ssv.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("Desktop cluster : %s is already deleted", ssv.getName()));
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, ssv);
        if (ssv.getState().equals(SSV.State.Stopped)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Desktop cluster : %s is already stopped", ssv.getName()));
            }
            return true;
        }
        if (ssv.getState().equals(SSV.State.Stopping)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Desktop cluster : %s is getting stopped", ssv.getName()));
            }
            return true;
        }
        SSVStopWorker stopWorker = new SSVStopWorker(ssv, this);
        stopWorker = ComponentContext.inject(stopWorker);
        return stopWorker.stop();
    }

    @Override
    public boolean deleteSSV(Long ssvId) throws CloudRuntimeException {
        if (!SSVEnabled.value()) {
            logAndThrow(Level.ERROR, "Desktop Service plugin is disabled");
        }
        SSVVO cluster = ssvDao.findById(ssvId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Invalid cluster id specified");
        }
        accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, cluster);
        SSVDestroyWorker destroyWorker = new SSVDestroyWorker(cluster, this);
        destroyWorker = ComponentContext.inject(destroyWorker);
        return destroyWorker.destroy();
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!SSVEnabled.value()) {
            return cmdList;
        }

        cmdList.add(ListAdminSSVCmd.class);
        cmdList.add(ListSSVCmd.class);
        cmdList.add(StartSSVCmd.class);
        cmdList.add(StopSSVCmd.class);
        cmdList.add(CreateSSVCmd.class);
        cmdList.add(DeleteSSVCmd.class);
        return cmdList;
    }

    @Override
    public SSV findById(final Long id) {
        return ssvDao.findById(id);
    }

    // Garbage collector periodically run through the Desktop clusters marked for GC. For each Desktop cluster
    // marked for GC, attempt is made to destroy cluster.
    public class SSVGarbageCollector extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("SSV.GC.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                List<SSVVO> ssvs = ssvDao.findSSVsToGarbageCollect();
                for (SSV ssv : ssvs) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Desktop cluster garbage collector on Desktop cluster : %s", ssv.getName()));
                    }
                    try {
                        SSVDestroyWorker destroyWorker = new SSVDestroyWorker(ssv, SSVManagerImpl.this);
                        destroyWorker = ComponentContext.inject(destroyWorker);
                        if (destroyWorker.destroy()) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info(String.format("Garbage collection complete for Desktop cluster : %s", ssv.getName()));
                            }
                        } else {
                            LOGGER.warn(String.format("Garbage collection failed for Desktop cluster : %s, it will be attempted to garbage collected in next run", ssv.getName()));
                        }
                    } catch (CloudRuntimeException e) {
                        LOGGER.warn(String.format("Failed to destroy Desktop cluster : %s during GC", ssv.getName()), e);
                        // proceed further with rest of the Desktop cluster garbage collection
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Desktop cluster gc: ", e);
            }
        }
    }

    /* Desktop cluster scanner checks if the Desktop cluster is in desired state. If it detects Desktop cluster
       is not in desired state, it will trigger an event and marks the Desktop cluster to be 'Alert' state. For e.g a
       Desktop cluster in 'Running' state should mean all the cluster of controller VM's in the custer should be running
       and the controller VM's is running. It is possible due to out of band changes by user or hosts going down,
       we may end up one or more VM's in stopped state. in which case scanner detects these changes and marks the cluster
       in 'Alert' state. Similarly cluster in 'Stopped' state means all the cluster VM's are in stopped state any mismatch
       in states should get picked up by Desktop cluster and mark the Desktop cluster to be 'Alert' state.
       Through recovery API, or reconciliation clusters in 'Alert' will be brought back to known good state or desired state.
     */
    public class SSVStatusScanner extends ManagedContextRunnable {
        private boolean firstRun = true;
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("SSV.State.Scanner.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                // run through Desktop clusters in 'Running' state and ensure all the VM's are Running in the cluster
                List<SSVVO> runningSSVs = ssvDao.findSSVsInState(SSV.State.Running);
                for (SSV ssv : runningSSVs) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Desktop cluster state scanner on Desktop cluster : %s",ssv.getName()));
                    }
                    try {
                        if (!isClusterVMsInDesiredState(ssv, VirtualMachine.State.Running)) {
                            stateTransitTo(ssv.getId(), SSV.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Desktop cluster Running state scanner on Desktop cluster : %s status scanner", ssv.getName()), e);
                    }
                }

                // run through Desktop clusters in 'Stopped' state and ensure all the VM's are Stopped in the cluster
                List<SSVVO> stoppedSSVs = ssvDao.findSSVsInState(SSV.State.Stopped);
                for (SSV ssv : stoppedSSVs) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Desktop cluster state scanner on Desktop cluster : %s for state: %s", ssv.getName(), SSV.State.Stopped.toString()));
                    }
                    try {
                        if (!isClusterVMsInDesiredState(ssv, VirtualMachine.State.Stopped)) {
                            stateTransitTo(ssv.getId(), SSV.Event.FaultsDetected);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Desktop cluster Stopped state scanner on Desktop cluster : %s status scanner", ssv.getName()), e);
                    }
                }

                // run through Desktop clusters in 'Alert' state and reconcile state as 'Running' if the VM's are running or 'Stopped' if VM's are stopped
                List<SSVVO> alertSSVs = ssvDao.findSSVsInState(SSV.State.Alert);
                for (SSVVO ssv : alertSSVs) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Running Desktop cluster state scanner on Desktop cluster : %s for state: %s", ssv.getName(), SSV.State.Alert.toString()));
                    }
                    try {
                        if (isClusterVMsInDesiredState(ssv, VirtualMachine.State.Running)) {
                            SSVStartWorker startWorker =
                                    new SSVStartWorker(ssv, SSVManagerImpl.this);
                            startWorker = ComponentContext.inject(startWorker);
                            startWorker.reconcileAlertCluster();
                        } else if (isClusterVMsInDesiredState(ssv, VirtualMachine.State.Stopped)) {
                            stateTransitTo(ssv.getId(), SSV.Event.StopRequested);
                            stateTransitTo(ssv.getId(), SSV.Event.OperationSucceeded);
                        }
                    } catch (Exception e) {
                        LOGGER.warn(String.format("Failed to run Desktop cluster Alert state scanner on Desktop cluster : %s status scanner", ssv.getName()), e);
                    }
                }


                if (firstRun) {
                    // run through Desktop clusters in 'Starting' state and reconcile state as 'Alert' or 'Error' if the VM's are running
                    List<SSVVO> startingSSVs = ssvDao.findSSVsInState(SSV.State.Starting);
                    for (SSV ssv : startingSSVs) {
                        if ((new Date()).getTime() - ssv.getCreated().getTime() < 10*60*1000) {
                            continue;
                        }
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format("Running Desktop cluster state scanner on Desktop cluster : %s for state: %s", ssv.getName(), SSV.State.Starting.toString()));
                        }
                        try {
                            if (isClusterVMsInDesiredState(ssv, VirtualMachine.State.Running)) {
                                stateTransitTo(ssv.getId(), SSV.Event.FaultsDetected);
                            } else {
                                stateTransitTo(ssv.getId(), SSV.Event.OperationFailed);
                            }
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to run Desktop cluster Starting state scanner on Desktop cluster : %s status scanner", ssv.getName()), e);
                        }
                    }
                    List<SSVVO> destroyingSSVs = ssvDao.findSSVsInState(SSV.State.Destroying);
                    for (SSV ssv : destroyingSSVs) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(String.format("Running Desktop cluster state scanner on Desktop cluster : %s for state: %s", ssv.getName(), SSV.State.Destroying.toString()));
                        }
                        try {
                            SSVDestroyWorker destroyWorker = new SSVDestroyWorker(ssv, SSVManagerImpl.this);
                            destroyWorker = ComponentContext.inject(destroyWorker);
                            destroyWorker.destroy();
                        } catch (Exception e) {
                            LOGGER.warn(String.format("Failed to run Desktop cluster Destroying state scanner on Desktop cluster : %s status scanner", ssv.getName()), e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Caught exception while running Desktop cluster state scanner", e);
            }
            firstRun = false;
        }
    }

    // checks if Desktop cluster is in desired state
    boolean isClusterVMsInDesiredState(SSV ssv, VirtualMachine.State state) {
        List<SSVVmMapVO> clusterVMs = ssvVmMapDao.listBySSVIdAndNotVmType(ssv.getId(), "desktopvm");

        // check if all the VM's are in same state
        for (SSVVmMapVO clusterVm : clusterVMs) {
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(clusterVm.getVmId());
            if (vm.getState() != state) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Found VM : %s in the Desktop cluster : %s in state: %s while expected to be in state: %s. So moving the cluster to Alert state for reconciliation",
                            vm.getUuid(), ssv.getName(), vm.getState().toString(), state.toString()));
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean start() {
        _gcExecutor.scheduleWithFixedDelay(new SSVGarbageCollector(), 300, 300, TimeUnit.SECONDS);
        _stateScanner.scheduleWithFixedDelay(new SSVStatusScanner(), 300, 30, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _gcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Desktop-Cluster-Scavenger"));
        _stateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Desktop-Cluster-State-Scanner"));

        return true;
    }

    @Override
    public String getConfigComponentName() {
        return SSVService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                SSVEnabled
        };
    }
}
