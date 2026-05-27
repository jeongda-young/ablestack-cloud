/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.vm;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface ImportVmTask extends Identity, InternalIdentity {
    String V2K_STEP_NONE = "None";

    enum MigrationTool {
        Legacy("legacy"),
        AblestackV2K("ablestack_v2k"),
        AblestackN2K("ablestack_n2k");

        private final String value;

        MigrationTool(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum SourceProvider {
        VMware("vmware"),
        Nutanix("nutanix"),
        KVM("kvm"),
        Local("local"),
        Shared("shared");

        private final String value;

        SourceProvider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum TargetProvider {
        Cloud("cloud"),
        KVM("kvm");

        private final String value;

        TargetProvider(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum MigrationPhase {
        Prepare("prepare"),
        Phase1("phase1"),
        Phase2("phase2"),
        Finalize("finalize"),
        Completed("completed");

        private final String value;

        MigrationPhase(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum MigrationState {
        Pending("pending"),
        Running("running"),
        Completed("completed"),
        Failed("failed");

        private final String value;

        MigrationState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum CredentialState {
        NotRequired("notrequired"),
        Managed("managed"),
        Stored("stored"),
        Legacy("legacy"),
        Missing("missing");

        private final String value;

        CredentialState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum Action {
        Refresh("refresh"),
        ClearCredentials("clearcredentials"),
        Phase2("phase2"),
        Finalize("finalize"),
        Retry("retry"),
        Resume("resume"),
        RetryFromStart("retryfromstart"),
        Cancel("cancel"),
        Delete("delete");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Action fromValue(String value) {
            for (Action action : Action.values()) {
                if (action.value.equalsIgnoreCase(value)) {
                    return action;
                }
            }
            throw new IllegalArgumentException("Invalid import VM task action: " + value);
        }
    }

    enum Step {
        Prepare, CloningInstance, ConvertingInstance, Importing, Completed
    }

    enum V2KStep {
        Phase1_In_Progress, Phase1_Completed, Phase2_In_Progress, Phase2_Completed, Completed
    }

    enum TaskState {
        Running, Completed, Failed, Cancelling, Cancelled;

        public static TaskState getValue(String state) {
            for (TaskState s : TaskState.values()) {
                if (s.name().equalsIgnoreCase(state)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid task state: " + state);
        }
    }
}
