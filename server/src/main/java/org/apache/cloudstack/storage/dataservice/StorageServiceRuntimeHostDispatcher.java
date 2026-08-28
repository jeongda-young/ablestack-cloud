// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
// See the NOTICE file distributed with this work for additional information regarding copyright ownership.
// Licensed under the Apache License, Version 2.0.
package org.apache.cloudstack.storage.dataservice;

import com.cloud.agent.api.StorageServiceRuntimeHostAnswer;
import com.cloud.agent.api.StorageServiceRuntimeHostCommand;

public interface StorageServiceRuntimeHostDispatcher {
    StorageServiceRuntimeHostAnswer dispatch(long vmId, StorageServiceRuntimeHostCommand command);
}
