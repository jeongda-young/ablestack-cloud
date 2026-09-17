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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.google.gson.Gson;
import org.apache.cloudstack.backup.AblestackCommvaultRestoreBackupCommand;
import org.apache.cloudstack.backup.AblestackNasRestoreBackupCommand;
import org.apache.cloudstack.backup.AblestackNetBackupRestoreBackupCommand;
import org.apache.cloudstack.backup.AblestackVeeamRestoreBackupCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class LibvirtAblestackRestoreJobMain {
    private static final Set<Class<? extends Command>> SUPPORTED_COMMANDS = Set.of(
            AblestackNasRestoreBackupCommand.class,
            AblestackCommvaultRestoreBackupCommand.class,
            AblestackNetBackupRestoreBackupCommand.class,
            AblestackVeeamRestoreBackupCommand.class);

    private LibvirtAblestackRestoreJobMain() {
    }

    public static void main(final String[] args) {
        int exitCode = 1;
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("restore command class and command file are required");
            }
            final Class<?> rawCommandClass = Class.forName(args[0]);
            if (!Command.class.isAssignableFrom(rawCommandClass)) {
                throw new IllegalArgumentException("Unsupported restore command class: " + args[0]);
            }
            @SuppressWarnings("unchecked")
            final Class<? extends Command> commandClass = (Class<? extends Command>) rawCommandClass;
            if (!SUPPORTED_COMMANDS.contains(commandClass)) {
                throw new IllegalArgumentException("Unsupported restore command class: " + args[0]);
            }
            final Command command = new Gson().fromJson(Files.readString(Path.of(args[1])), commandClass);
            enableSynchronousExecution(command);
            final LibvirtComputingResource resource = new LibvirtComputingResource();
            if (!resource.configureForDetachedRestore()) {
                throw new IllegalStateException("Unable to configure KVM resource for detached restore job");
            }
            final Answer answer = resource.executeRequest(command);
            exitCode = answer != null && answer.getResult() ? 0 : 1;
        } catch (Exception e) {
            System.err.println("Detached restore job failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        System.exit(exitCode);
    }

    private static void enableSynchronousExecution(final Command command) {
        if (command instanceof AblestackNasRestoreBackupCommand) {
            ((AblestackNasRestoreBackupCommand) command).setWaitForCompletion(true);
        } else if (command instanceof AblestackCommvaultRestoreBackupCommand) {
            ((AblestackCommvaultRestoreBackupCommand) command).setWaitForCompletion(true);
        } else if (command instanceof AblestackNetBackupRestoreBackupCommand) {
            ((AblestackNetBackupRestoreBackupCommand) command).setWaitForCompletion(true);
        } else if (command instanceof AblestackVeeamRestoreBackupCommand) {
            ((AblestackVeeamRestoreBackupCommand) command).setWaitForCompletion(true);
        }
    }
}
