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

package com.cloud.ssv.actionworkers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Level;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;
import com.cloud.uservm.UserVm;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.server.ServerProperties;
import com.cloud.ssv.SSV;
import com.cloud.ssv.SSVVmMapVO;
import com.cloud.ssv.SSVManagerImpl;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.api.query.vo.UserAccountJoinVO;

public class SSVStartWorker extends SSVModifierActionWorker {

    // private DesktopControllerVersion ssvVersion;
    private static final long GiB_TO_BYTES = 1024 * 1024 * 1024;

    public SSVStartWorker(final SSV ssv, final SSVManagerImpl clusterManager) {
        super(ssv, clusterManager);
    }

    // public DesktopControllerVersion getSSVVersion() {
    //     if (ssvVersion == null) {
    //         ssvVersion = desktopControllerVersionDao.findById(ssv.getDesktopVersionId());
    //     }
    //     return ssvVersion;
    // }
    private String getSSVWorksConfig(final DataCenter zone) throws IOException {
        String[] keys = getServiceUserKeys(owner);
        String[] info = getServerProperties();
        final String managementIp = ApiServiceConfiguration.ManagementServerAddresses.value();
        String ssvWorksConfig = readResourceFile("/conf/works");
        final String clusterName = "{{ cluster_name }}";
        final String domainName = "{{ domain_name }}";
        final String worksIp = "{{ works_ip }}";
        final String dcIp = "{{ dc_ip }}";
        final String zoneUuid = "{{ zone_id }}";
        final String networkId = "{{ network_id }}";
        final String accountName = "{{ account_name }}";
        final String domainUuid = "{{ domain_uuid }}";
        final String apiKey = "{{ api_key }}";
        final String secretKey = "{{ secret_key }}";
        final String moldIp = "{{ mold_ip }}";
        final String moldPort = "{{ mold_port }}";
        final String moldProtocol = "{{ mold_protocol }}";
        List<UserAccountJoinVO> domain = userAccountJoinDao.searchByAccountId(owner.getId());
        ssvWorksConfig = ssvWorksConfig.replace(clusterName, ssv.getName());
        ssvWorksConfig = ssvWorksConfig.replace(domainName, ssv.getAdDomainName());
        ssvWorksConfig = ssvWorksConfig.replace(worksIp, ssv.getWorksIp());
        ssvWorksConfig = ssvWorksConfig.replace(dcIp, ssv.getDcIp());
        ssvWorksConfig = ssvWorksConfig.replace(zoneUuid, zone.getUuid());
        ssvWorksConfig = ssvWorksConfig.replace(networkId, Long.toString(ssv.getNetworkId()));
        ssvWorksConfig = ssvWorksConfig.replace(accountName, owner.getAccountName());
        ssvWorksConfig = ssvWorksConfig.replace(domainUuid, domain.get(0).getDomainUuid());
        ssvWorksConfig = ssvWorksConfig.replace(apiKey, keys[0]);
        ssvWorksConfig = ssvWorksConfig.replace(secretKey, keys[1]);
        ssvWorksConfig = ssvWorksConfig.replace(moldIp, managementIp);
        ssvWorksConfig = ssvWorksConfig.replace(moldPort, info[0]);
        ssvWorksConfig = ssvWorksConfig.replace(moldProtocol, info[1]);
        String base64UserData = Base64.encodeBase64String(ssvWorksConfig.getBytes(StringUtils.getPreferredCharset()));
        String ssvWorksEncodeConfig = readResourceFile("/conf/works.yml");
        final String worksEncode = "{{ works_encode }}";
        ssvWorksEncodeConfig = ssvWorksEncodeConfig.replace(worksEncode, base64UserData);
        return ssvWorksEncodeConfig;
    }

    private UserVm provisionSSVWorksControlVm(final Network network) throws
            InsufficientCapacityException, ManagementServerException, ResourceUnavailableException {
        UserVm worksControlVm = null;
        final String type = "worksvm";
        worksControlVm = createSSVWorksControlVm(network);
        addSSVVm(ssv.getId(), worksControlVm.getId(), type);
        startDesktopVM(worksControlVm);
        worksControlVm = userVmDao.findById(worksControlVm.getId());
        if (worksControlVm == null) {
            throw new ManagementServerException(String.format("Failed to provision Works Control VM for desktop cluster : %s" , ssv.getName()));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Provisioned Works Control VM : %s in to the desktop cluster : %s", worksControlVm.getDisplayName(), ssv.getName()));
        }
        return worksControlVm;
    }


    private UserVm createSSVWorksControlVm(final Network network) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException {
        UserVm worksControlVm = null;
        LinkedHashMap<Long, IpAddresses> ipToNetworkMap = null;
        DataCenter zone = dataCenterDao.findById(ssv.getZoneId());
        ServiceOffering serviceOffering = serviceOfferingDao.findById(ssv.getServiceOfferingId());
        List<Long> networkIds = new ArrayList<Long>();
        networkIds.add(ssv.getNetworkId());
        String worksIp = ssv.getWorksIp();
        String reName = ssv.getName();
        String hostName = reName + "-works";
        Map<String, String> customParameterMap = new HashMap<String, String>();
        DiskOfferingVO diskOffering = diskOfferingDao.findById(serviceOffering.getId());
        long rootDiskSizeInBytes = diskOffering.getDiskSize();
        if (rootDiskSizeInBytes > 0) {
            long rootDiskSizeInGiB = rootDiskSizeInBytes / GiB_TO_BYTES;
            customParameterMap.put("rootdisksize", String.valueOf(rootDiskSizeInGiB));
        }
        String ssvWorksConfig = null;
        try {
            ssvWorksConfig = getSSVWorksConfig(zone);
        } catch (IOException e) {
            logAndThrow(Level.ERROR, "Failed to read Desktop Cluster Userdata configuration file", e);
        }
        String base64UserData = Base64.encodeBase64String(ssvWorksConfig.getBytes(StringUtils.getPreferredCharset()));
        List<String> keypairs = new ArrayList<String>(); // 키페어 파라메타 임시 생성
        if (worksIp == null || network.getGuestType() == Network.GuestType.L2) {
            Network.IpAddresses addrs = new Network.IpAddresses(null, null, null);
            worksControlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, worksTemplate, networkIds, owner,
                hostName, hostName, null, null, null,
                worksTemplate.getHypervisorType(), BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                null, addrs, null, null, null, customParameterMap, null, null, null, null, true, null, null);
        } else {
            ipToNetworkMap = new LinkedHashMap<Long, IpAddresses>();
            Network.IpAddresses addrs = new Network.IpAddresses(null, null, null);
            Network.IpAddresses worksAddrs = new Network.IpAddresses(worksIp, null, null);
            ipToNetworkMap.put(ssv.getNetworkId(), worksAddrs);
            worksControlVm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, worksTemplate, networkIds, owner,
                hostName, hostName, null, null, null,
                worksTemplate.getHypervisorType(), BaseCmd.HTTPMethod.POST, base64UserData, null, null, keypairs,
                ipToNetworkMap, addrs, null, null, null, customParameterMap, null, null, null, null, true, null, null);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Created Control VM ID : %s, %s in the desktop cluster : %s", worksControlVm.getUuid(), hostName, ssv.getName()));
        }
        return worksControlVm;
    }

    private Network startSSVNetwork(final DeployDestination destination) throws ManagementServerException {
        final ReservationContext context = new ReservationContextImpl(null, null, null, owner);
        Network network = networkDao.findById(ssv.getNetworkId());
        if (network == null) {
            String msg  = String.format("Network for desktop cluster : %s not found", ssv.getName());
            LOGGER.warn(msg);
            stateTransitTo(ssv.getId(), SSV.Event.CreateFailed);
            throw new ManagementServerException(msg);
        }
        try {
            networkMgr.startNetwork(network.getId(), destination, context);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Network : %s is started for the desktop cluster : %s", network.getName(), ssv.getName()));
            }
        } catch (ConcurrentOperationException | ResourceUnavailableException |InsufficientCapacityException e) {
            String msg = String.format("Failed to start desktop cluster : %s as unable to start associated network : %s" , ssv.getName(), network.getName());
            LOGGER.error(msg, e);
            stateTransitTo(ssv.getId(), SSV.Event.CreateFailed);
            throw new ManagementServerException(msg, e);
        }
        return network;
    }

    private void startSSVVMs() {
        List <UserVm> clusterVms = getControlVMs();
        for (final UserVm vm : clusterVms) {
            if (vm == null) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Control VMs in desktop cluster : %s", ssv.getName()), ssv.getId(), SSV.Event.OperationFailed);
            }
            try {
                startDesktopVM(vm);
            } catch (ManagementServerException ex) {
                LOGGER.warn(String.format("Failed to start VM : %s in desktop cluster : %s due to ", vm.getDisplayName(), ssv.getName()) + ex);
                // dont bail out here. proceed further to stop the reset of the VM's
            }
        }
        for (final UserVm userVm : clusterVms) {
            UserVm vm = userVmDao.findById(userVm.getId());
            if (vm == null || !vm.getState().equals(VirtualMachine.State.Running)) {
                logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Control VMs in desktop cluster : %s", ssv.getName()), ssv.getId(), SSV.Event.OperationFailed);
            }
        }
    }

    private String[] getServiceUserKeys(Account owner) {
        if (owner == null) {
            owner = CallContext.current().getCallingAccount();
        }
        String username = owner.getAccountName();
        UserAccount user = accountService.getActiveUserAccount(username, owner.getDomainId());
        String[] keys = null;
        String apiKey = user.getApiKey();
        String secretKey = user.getSecretKey();
        if ((apiKey == null || apiKey.length() == 0) || (secretKey == null || secretKey.length() == 0)) {
            keys = accountService.createApiKeyAndSecretKey(user.getId());
        } else {
            keys = new String[]{apiKey, secretKey};
        }
        return keys;
    }

    private String[] getServerProperties() {
        String[] serverInfo = null;
        final String HTTP_PORT = "http.port";
        final String HTTPS_ENABLE = "https.enable";
        final String HTTPS_PORT = "https.port";
        final File confFile = PropertiesUtil.findConfigFile("server.properties");
        try {
            InputStream is = new FileInputStream(confFile);
            String port = null;
            String protocol = null;
            final Properties properties = ServerProperties.getServerProperties(is);
            if (properties.getProperty(HTTPS_ENABLE).equals("true")){
                port = properties.getProperty(HTTPS_PORT);
                protocol = "https://";
            } else {
                port = properties.getProperty(HTTP_PORT);
                protocol = "http://";
            }
            serverInfo = new String[]{port, protocol};
        } catch (final IOException e) {
            LOGGER.warn("Failed to read configuration from server.properties file", e);
        }
        return serverInfo;
    }

    private boolean setupSSVNetworkRules(Network network, UserVm worksVm, IpAddress publicIp) throws ManagementServerException {
        boolean egress = false;
        boolean firewall = false;
        boolean firewall2 = false;
        boolean portForwarding = false;
        // Firewall Egress Network
        try {
            egress = provisionEgressFirewallRules(network, owner, CLUSTER_PORTAL_PORT, CLUSTER_LITE_PORT);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Provisioned egress firewall rule to open up port %d to %d on %s for Desktop cluster : %s", CLUSTER_PORTAL_PORT, CLUSTER_LITE_PORT, publicIp.getAddress().addr(), ssv.getName()));
            }
        } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
            throw new ManagementServerException(String.format("Failed to provision egress firewall rules for Web access for the Desktop cluster : %s", ssv.getName()), e);
        }
        // Firewall rule fo Web access on WorksVM
        if (egress) {
            try {
                firewall = provisionFirewallRules(publicIp, owner, CLUSTER_PORTAL_PORT, CLUSTER_API_PORT);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Desktop cluster : %s", CLUSTER_PORTAL_PORT, CLUSTER_API_PORT, publicIp.getAddress().addr(), ssv.getName()));
                }
                firewall2 = provisionFirewallRules(publicIp, owner, CLUSTER_SAMBA_PORT, CLUSTER_SAMBA_PORT);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Provisioned firewall rule to open up port %d to %d on %s for Desktop cluster : %s", CLUSTER_SAMBA_PORT, CLUSTER_SAMBA_PORT, publicIp.getAddress().addr(), ssv.getName()));
                }
            } catch (NoSuchFieldException | IllegalAccessException | ResourceUnavailableException | NetworkRuleConflictException e) {
                throw new ManagementServerException(String.format("Failed to provision firewall rules for Web access for the Desktop cluster : %s", ssv.getName()), e);
            }
            if (firewall && firewall2) {
                // Port forwarding rule fo Web access on WorksVM
                try {
                    portForwarding = provisionPortForwardingRules(publicIp, network, owner, worksVm, CLUSTER_LITE_PORT, CLUSTER_PORTAL_PORT, CLUSTER_SAMBA_PORT, CLUSTER_API_PORT);
                } catch (ResourceUnavailableException | NetworkRuleConflictException e) {
                    throw new ManagementServerException(String.format("Failed to activate Web port forwarding rules for the Desktop cluster : %s", ssv.getName()), e);
                }
                if (portForwarding) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean startSSVOnCreate() {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Starting Desktop cluster : %s", ssv.getName()));
        }
        stateTransitTo(ssv.getId(), SSV.Event.StartRequested);
        DeployDestination dest = null;
        try {
            dest = plan();
        } catch (InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the cluster failed due to insufficient capacity in the desktop cluster: %s", ssv.getUuid()), ssv.getId(), SSV.Event.CreateFailed, e);
        }
        Network network = null;
        try {
            network = startSSVNetwork(dest);
        } catch (ManagementServerException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start desktop cluster : %s as its network cannot be started", ssv.getName()), ssv.getId(), SSV.Event.CreateFailed, e);
        }
        IpAddress publicIpAddress = null;
        publicIpAddress = getSSVServerIp();
        if (publicIpAddress == null) {
            logTransitStateAndThrow(Level.ERROR, String.format("Failed to start Desktop cluster : %s as no public IP found for the cluster" , ssv.getName()), ssv.getId(), SSV.Event.CreateFailed);
        }
        List<UserVm> clusterVMs = new ArrayList<>();
        UserVm worksVM = null;
        try {
            worksVM = provisionSSVWorksControlVm(network);
        }  catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the Works Control VM failed in the desktop cluster : %s, %s", ssv.getName(), e), ssv.getId(), SSV.Event.CreateFailed, e);
        }
        clusterVMs.add(worksVM);
        if (worksVM.getState().equals(VirtualMachine.State.Running)) {
            // boolean setup = false;
            // try {
            //     setup = setupSSVNetworkRules(network, worksVM, publicIpAddress);
            // } catch (ManagementServerException e) {
            //     logTransitStateAndThrow(Level.ERROR, String.format("Failed to setup Desktop cluster : %s, unable to setup network rules", ssv.getName()), ssv.getId(), SSV.Event.CreateFailed, e);
            // }
            // if (setup) {
            //     try {
            //         if (callApi(publicIpAddress.getAddress().addr())) {
            //             UserVm dcVM = null;
            //             try {
            //                 dcVM = provisionSSVDcControlVm(network);
            //             } catch (CloudRuntimeException | ManagementServerException | ResourceUnavailableException | InsufficientCapacityException e) {
            //                 logTransitStateAndThrow(Level.ERROR, String.format("Provisioning the DC Control VM failed in the desktop cluster : %s, %s", ssv.getName(), e), ssv.getId(), SSV.Event.CreateFailed, e);
            //             }
            //             clusterVMs.add(dcVM);
            //             if (LOGGER.isInfoEnabled()) {
            //                 LOGGER.info(String.format("Desktop cluster : %s Control VMs successfully provisioned", ssv.getName()));
            //             }
            //             stateTransitTo(ssv.getId(), SSV.Event.OperationSucceeded);
            //             return true;
            //         }
            //     } catch (IOException | InterruptedException e) {
            //         logTransitStateAndThrow(Level.ERROR, String.format("Provisioning failed in the desktop cluster : %s, %s", ssv.getName(), e), ssv.getId(), SSV.Event.CreateFailed, e);
            //     }
            // }
        }
        return false;
    }

    public boolean startStoppedSSV() throws CloudRuntimeException {
        init();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Starting desktop cluster : %s", ssv.getName()));
        }
        stateTransitTo(ssv.getId(), SSV.Event.StartRequested);
        startSSVVMs();
        stateTransitTo(ssv.getId(), SSV.Event.OperationSucceeded);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Desktop cluster : %s successfully started", ssv.getName()));
        }
        return true;
    }

    public boolean reconcileAlertCluster() {
        init();
        List<SSVVmMapVO> vmMapVOList = getControlVMMaps();
        if (CollectionUtils.isEmpty(vmMapVOList)) {
            return false;
        }
        // mark the cluster to be running
        stateTransitTo(ssv.getId(), SSV.Event.RecoveryRequested);
        stateTransitTo(ssv.getId(), SSV.Event.OperationSucceeded);
        return true;
    }

    public boolean callApi(String sambaIp) throws InterruptedException, IOException {
        int tryCount = 0;
        HttpURLConnection conn = null;
        while (tryCount < 10) {
            Thread.sleep(60000);
            try {
                URL url = new URL("http://"+sambaIp+":9017/api/v1/version");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setConnectTimeout(180000);
                conn.setReadTimeout(180000);
                conn.setDoOutput(true);
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    return true;
                }
            } catch (ConnectException e) {
                tryCount++;
                if (tryCount > 8) {
                    logTransitStateAndThrow(Level.ERROR, String.format("DC Control VM could not be deployed because Works API call failed. : %s, %s", ssv.getName(), e), ssv.getId(), SSV.Event.CreateFailed, e);
                }
            }
        }
        return false;
    }
}
