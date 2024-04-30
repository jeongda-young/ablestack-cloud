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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NicLinkStateCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  NicLinkStateCommand.class)
public final class LibvirtNicLinkStateCommandWrapper extends CommandWrapper<NicLinkStateCommand, Answer, LibvirtComputingResource> {

    public enum DomainAffect {
        CURRENT(0), LIVE(1), CONFIG(2), BOTH(3);

        private int value;
        DomainAffect(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public Answer execute(final NicLinkStateCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final NicTO nic = command.getNic();
        final String vmName = command.getInstanceName();
        final Boolean linkState = command.getLinkState();
        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            InterfaceDef oldPluggedNic = findPluggedNic(libvirtComputingResource, nic, vmName, conn);

            final VifDriver newVifDriver = libvirtComputingResource.getVifDriver(nic.getType(), nic.getName());
            final InterfaceDef interfaceDef = newVifDriver.plug(nic, "Other PV", oldPluggedNic.getModel().toString(), null);
            // if (command.getDetails() != null) {
            //     libvirtComputingResource.setInterfaceDefQueueSettings(command.getDetails(), null, interfaceDef);
            // }
            interfaceDef.setSlot(oldPluggedNic.getSlot());
            interfaceDef.setDevName(oldPluggedNic.getDevName());
            interfaceDef.setLinkStateUp(linkState);

            oldPluggedNic.setSlot(null);

            vm.detachDevice(oldPluggedNic.toString());
            logger.debug("Update Nic Link State: Attaching interface" + interfaceDef);
            vm.attachDevice(interfaceDef.toString());


            logger.debug("Update Nic Link State: Updating interface" + interfaceDef);
            // vm.updateDeviceFlags(interfaceDef.toString(), DomainAffect.CURRENT.getValue());
            // We don't know which "traffic type" is associated with
            // each interface at this point, so inform all vif drivers
            // for (final VifDriver vifDriver : libvirtComputingResource.getAllVifDrivers()) {
            //     vifDriver.unplug(oldPluggedNic, true);
            // }

            return new Answer(command, true, "success");
        } catch (final LibvirtException | InternalErrorException e) {
            final String msg = " Update Nic Link State failed due to " + e.toString();
            logger.warn(msg, e);
            return new Answer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }

    private InterfaceDef findPluggedNic(LibvirtComputingResource libvirtComputingResource, NicTO nic, String vmName, Connect conn) {
        InterfaceDef oldPluggedNic = null;

        final List<InterfaceDef> pluggedNics = libvirtComputingResource.getInterfaces(conn, vmName);

        for (final InterfaceDef pluggedNic : pluggedNics) {
            if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                oldPluggedNic = pluggedNic;
            }
        }

        return oldPluggedNic;
    }
}
