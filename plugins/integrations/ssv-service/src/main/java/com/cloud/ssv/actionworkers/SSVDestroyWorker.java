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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Level;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ssv.SSV;
import com.cloud.ssv.SSVVO;
import com.cloud.ssv.SSVVmMapVO;
import com.cloud.ssv.SSVVmMap;
import com.cloud.ssv.SSVManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.IpAddress;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;

public class SSVDestroyWorker extends SSVModifierActionWorker {

    @Inject
    protected AccountManager accountManager;
    @Inject
    protected ResourceTagDao resourceTagDao;

    private List<SSVVmMapVO> clusterVMs;

    public SSVDestroyWorker(final SSV ssv, final SSVManagerImpl clusterManager) {
        super(ssv, clusterManager);
    }

    private void validateClusterState() {
        if (!(ssv.getState().equals(SSV.State.Running)
                || ssv.getState().equals(SSV.State.Stopped)
                || ssv.getState().equals(SSV.State.Alert)
                || ssv.getState().equals(SSV.State.Error)
                || ssv.getState().equals(SSV.State.Destroying))) {
            String msg = String.format("Cannot perform delete operation on cluster : %s in state: %s",
            ssv.getName(), ssv.getState());
            LOGGER.warn(msg);
            throw new PermissionDeniedException(msg);
        }
    }

    private boolean destroyClusterVMs() {
        boolean vmDestroyed = true;
        //ControlVM removed
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (SSVVmMapVO clusterVM : clusterVMs) {
                long vmID = clusterVM.getVmId();

                // delete only if VM exists and is not removed
                UserVmVO userVM = userVmDao.findById(vmID);
                if (userVM == null || userVM.isRemoved()) {
                    continue;
                }
                try {
                    UserVm vm = userVmService.destroyVm(vmID, true);
                    if (!userVmManager.expunge(userVM)) {
                        LOGGER.warn(String.format("Unable to expunge VM %s : %s, destroying desktop cluster will probably fail",
                            vm.getInstanceName() , vm.getUuid()));
                    }
                    ssvVmMapDao.expunge(clusterVM.getId());
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Destroyed VM : %s as part of desktop cluster : %s cleanup", vm.getDisplayName(), ssv.getName()));
                    }
                } catch (ResourceUnavailableException | ConcurrentOperationException e) {
                    LOGGER.warn(String.format("Failed to destroy VM : %s part of the desktop cluster : %s cleanup. Moving on with destroying remaining resources provisioned for the desktop cluster", userVM.getDisplayName(), ssv.getName()), e);
                    return false;
                }
            }
            //DesktopVM removed
            List<VMInstanceVO> vmList = vmInstanceDao.listByZoneId(ssv.getZoneId());
            String resourceKey = "ClusterName";
            if (vmList != null && !vmList.isEmpty()) {
                for (VMInstanceVO vmVO : vmList) {
                    ResourceTag desktopvm = resourceTagDao.findByKey(vmVO.getId(), ResourceObjectType.UserVm, resourceKey);
                    if (desktopvm != null) {
                        if (desktopvm.getValue().equals(ssv.getName())) {
                            long desktopvmID = vmVO.getId();
                            // delete only if VM exists and is not removed
                            UserVmVO userDesktopVM = userVmDao.findById(desktopvmID);
                            if (userDesktopVM == null || userDesktopVM.isRemoved()) {
                                continue;
                            }
                            try {
                                UserVm deskvm = userVmService.destroyVm(desktopvmID, true);
                                if (!userVmManager.expunge(userDesktopVM)) {
                                    LOGGER.warn(String.format("Unable to expunge VM %s : %s, Destroying a desktop virtual machine in a desktop cluster will probably fail",
                                    deskvm.getInstanceName() , deskvm.getUuid()));
                                }
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info(String.format("Destroyed VM : %s as part of desktop cluster : %s desktop virtual machine cleanup", deskvm.getDisplayName(), ssv.getName()));
                                }
                            } catch (ResourceUnavailableException | ConcurrentOperationException e) {
                                LOGGER.warn(String.format("Failed to destroy VM : %s part of the desktop cluster : %s desktop virtual machine cleanup. Moving on with destroying remaining resources provisioned for the desktop cluster", userDesktopVM.getDisplayName(), ssv.getName()), e);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return vmDestroyed;
    }

    private boolean updateSSVEntryForGC() {
        SSVVO ssvVO = ssvDao.findById(ssv.getId());
        ssvVO.setCheckForGc(true);
        return ssvDao.update(ssv.getId(), ssvVO);
    }

    private void deleteSSVNetworkRules() throws ManagementServerException {
        NetworkVO network = networkDao.findById(ssv.getNetworkId());
        if (network == null) {
            return;
        }
        List<Long> removedVmIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(clusterVMs)) {
            for (SSVVmMapVO clusterVM : clusterVMs) {
                removedVmIds.add(clusterVM.getVmId());
            }
        }
        IpAddress publicIp = getSourceNatIp(network);
        if (publicIp == null) {
            throw new ManagementServerException(String.format("No source NAT IP addresses found for network : %s", network.getName()));
        }
        removeFirewallIngressRule(publicIp);
        removeFirewallEgressRule(network);
        try {
            removePortForwardingRules(publicIp, network, owner, removedVmIds);
        } catch (ResourceUnavailableException e) {
            // throw new ManagementServerException(String.format("Failed to desktop cluster port forwarding rules for network : %s", network.getName()));
        }
    }

    private void validateClusterVMsDestroyed() {
        if(clusterVMs!=null  && !clusterVMs.isEmpty()) { // Wait for few seconds to get all VMs really expunged
            final int maxRetries = 3;
            int retryCounter = 0;
            while (retryCounter < maxRetries) {
                boolean allVMsRemoved = true;
                for (SSVVmMap clusterVM : clusterVMs) {
                    UserVmVO userVM = userVmDao.findById(clusterVM.getVmId());
                    if (userVM != null && !userVM.isRemoved()) {
                        allVMsRemoved = false;
                        break;
                    }
                }
                if (allVMsRemoved) {
                    break;
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {}
                retryCounter++;
            }
        }
    }

    private void checkForRulesToDelete() throws ManagementServerException {
        NetworkVO ssvNetwork = networkDao.findById(ssv.getNetworkId());
        if (ssvNetwork != null && ssvNetwork.getGuestType() != Network.GuestType.Shared) {
            deleteSSVNetworkRules();
        }
    }

    private boolean destroyClusterIps() {
        boolean ipDestroyed = true;
        // List<SSVIpRangeVO> ipRangeList = ssvIpRangeDao.listBySSVId(ssv.getId());
        //     for (SSVIpRangeVO iprange : ipRangeList) {
        //         boolean deletedIp = ssvIpRangeDao.remove(iprange.getId());
        //         if (!deletedIp) {
        //             logMessage(Level.WARN, String.format("Failed to delete desktop cluster ip range : %s", ssv.getName()), null);
        //             return false;
        //         }
        //         if (LOGGER.isInfoEnabled()) {
        //             LOGGER.info(String.format("Desktop cluster ip range : %s is successfully deleted", ssv.getName()));
        //         }
        //     }
        return ipDestroyed;
    }

    public boolean destroy() throws CloudRuntimeException {
        init();
        validateClusterState();
        this.clusterVMs = ssvVmMapDao.listBySSVId(ssv.getId());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Destroying desktop cluster : %s", ssv.getName()));
        }
        stateTransitTo(ssv.getId(), SSV.Event.DestroyRequested);
        boolean vmsDestroyed = destroyClusterVMs();
        // if there are VM's that were not expunged, we can not delete the network
        if (vmsDestroyed) {
            validateClusterVMsDestroyed();
            try {
                checkForRulesToDelete();
            } catch (ManagementServerException e) {
                String msg = String.format("Failed to remove network rules of desktop cluster : %s", ssv.getName());
                LOGGER.warn(msg, e);
                updateSSVEntryForGC();
                throw new CloudRuntimeException(msg, e);
            }
        } else {
            String msg = String.format("Failed to destroy one or more VMs as part of desktop cluster : %s cleanup",ssv.getName());
            LOGGER.warn(msg);
            updateSSVEntryForGC();
            throw new CloudRuntimeException(msg);
        }
        stateTransitTo(ssv.getId(), SSV.Event.OperationSucceeded);
        final String accessType = "internal";
        // Desktop Cluster IP Range remove
        if (ssv.getAccessType().equals(accessType)) {
            boolean ipDestroyed = destroyClusterIps();
        }
        boolean deleted = ssvDao.remove(ssv.getId());
        if (!deleted) {
            logMessage(Level.WARN, String.format("Failed to delete desktop cluster : %s", ssv.getName()), null);
            updateSSVEntryForGC();
            return false;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Desktop cluster : %s is successfully deleted", ssv.getName()));
        }
        return true;
    }
}
