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
package com.cloud.agent.api;

public class FtctlDrSourceHardwareAnswer extends Answer {
    private String sourceVmRef;
    private String firmware;
    private Boolean secureBoot;
    private String guestId;
    private Integer cpuCount;
    private Long memoryMiB;
    private String rootDiskController;
    private String dataDiskController;
    private String inventorySource;

    public FtctlDrSourceHardwareAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public String getSourceVmRef() { return sourceVmRef; }
    public void setSourceVmRef(String sourceVmRef) { this.sourceVmRef = sourceVmRef; }
    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }
    public Boolean getSecureBoot() { return secureBoot; }
    public void setSecureBoot(Boolean secureBoot) { this.secureBoot = secureBoot; }
    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public Integer getCpuCount() { return cpuCount; }
    public void setCpuCount(Integer cpuCount) { this.cpuCount = cpuCount; }
    public Long getMemoryMiB() { return memoryMiB; }
    public void setMemoryMiB(Long memoryMiB) { this.memoryMiB = memoryMiB; }
    public String getRootDiskController() { return rootDiskController; }
    public void setRootDiskController(String rootDiskController) { this.rootDiskController = rootDiskController; }
    public String getDataDiskController() { return dataDiskController; }
    public void setDataDiskController(String dataDiskController) { this.dataDiskController = dataDiskController; }
    public String getInventorySource() { return inventorySource; }
    public void setInventorySource(String inventorySource) { this.inventorySource = inventorySource; }
}
