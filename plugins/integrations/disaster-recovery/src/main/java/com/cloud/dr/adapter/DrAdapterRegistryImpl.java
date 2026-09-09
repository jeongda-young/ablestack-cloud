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
package com.cloud.dr.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.ManagerBase;

public class DrAdapterRegistryImpl extends ManagerBase implements DrAdapterRegistry {
    private final Map<String, DrReplicationEngine> replicationEngines = new HashMap<String, DrReplicationEngine>();
    private final Map<String, DrFencingAdapter> fencingAdapters = new HashMap<String, DrFencingAdapter>();
    private final Map<String, DrProjectionAdapter> projectionAdapters = new HashMap<String, DrProjectionAdapter>();

    public void setReplicationEngines(List<DrReplicationEngine> engines) {
        if (engines != null) {
            for (DrReplicationEngine engine : engines) {
                registerReplicationEngine(engine);
            }
        }
    }

    public void setFencingAdapters(List<DrFencingAdapter> adapters) {
        if (adapters != null) {
            for (DrFencingAdapter adapter : adapters) {
                registerFencingAdapter(adapter);
            }
        }
    }

    public void setProjectionAdapters(List<DrProjectionAdapter> adapters) {
        if (adapters != null) {
            for (DrProjectionAdapter adapter : adapters) {
                registerProjectionAdapter(adapter);
            }
        }
    }

    @Override
    public DrReplicationEngine getReplicationEngine(String engineType, String engineBindingType) {
        return replicationEngines.get(adapterKey(engineType, engineBindingType));
    }

    @Override
    public DrFencingAdapter getFencingAdapter(String engineType, String engineBindingType) {
        return fencingAdapters.get(adapterKey(engineType, engineBindingType));
    }

    @Override
    public DrProjectionAdapter getProjectionAdapter(String engineType, String engineBindingType) {
        return projectionAdapters.get(adapterKey(engineType, engineBindingType));
    }

    @Override
    public List<DrReplicationEngine> listReplicationEngines() {
        return new ArrayList<DrReplicationEngine>(replicationEngines.values());
    }

    @Override
    public void registerReplicationEngine(DrReplicationEngine engine) {
        if (engine != null) {
            replicationEngines.put(adapterKey(engine.getEngineType(), engine.getEngineBindingType()), engine);
        }
    }

    @Override
    public void registerFencingAdapter(DrFencingAdapter adapter) {
        if (adapter != null) {
            fencingAdapters.put(adapterKey(adapter.getEngineType(), adapter.getEngineBindingType()), adapter);
        }
    }

    @Override
    public void registerProjectionAdapter(DrProjectionAdapter adapter) {
        if (adapter != null) {
            projectionAdapters.put(adapterKey(adapter.getEngineType(), adapter.getEngineBindingType()), adapter);
        }
    }

    private String adapterKey(String engineType, String engineBindingType) {
        return StringUtils.defaultString(engineType).toUpperCase() + ":" + StringUtils.defaultString(engineBindingType).toUpperCase();
    }
}
