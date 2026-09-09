// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
package com.cloud.dr;

import org.apache.cloudstack.api.response.dr.DrVmProtectionViewResponse;

public interface DrVmProtectionViewService {
    DrVmProtectionViewResponse getView(long vmId);
}
