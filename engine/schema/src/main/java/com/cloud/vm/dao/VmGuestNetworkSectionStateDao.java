// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package com.cloud.vm.dao;

import java.util.Collection;
import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VmGuestNetworkSectionStateVO;

public interface VmGuestNetworkSectionStateDao
        extends GenericDao<VmGuestNetworkSectionStateVO, Long> {
    List<VmGuestNetworkSectionStateVO> listByVmId(long vmId);
    List<VmGuestNetworkSectionStateVO> listByVmIds(Collection<Long> vmIds);
    VmGuestNetworkSectionStateVO findByVmIdAndSection(long vmId, String section);
}
