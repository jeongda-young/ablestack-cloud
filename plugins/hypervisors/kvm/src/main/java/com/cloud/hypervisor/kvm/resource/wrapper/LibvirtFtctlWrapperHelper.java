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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.cloud.utils.script.OutputInterpreter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;

public final class LibvirtFtctlWrapperHelper {

    static final class FtctlProcessOutputParser extends OutputInterpreter.AllLinesParser {
        private static final long DRAIN_WAIT_SECONDS = 5;

        private final CountDownLatch drained = new CountDownLatch(1);
        private volatile String lines;
        private volatile String readFailure;

        @Override
        public String interpret(BufferedReader reader) {
            StringBuilder builder = new StringBuilder();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                lines = builder.toString();
            } catch (IOException e) {
                readFailure = e.getMessage();
            } finally {
                drained.countDown();
            }
            return null;
        }

        @Override
        public String processError(BufferedReader reader) {
            try {
                if (!drained.await(DRAIN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    return "FTCTL process output drain timed out";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "FTCTL process output drain was interrupted";
            }
            return StringUtils.defaultIfBlank(lines, StringUtils.defaultString(readFailure));
        }

        @Override
        public String getLines() {
            return lines;
        }
    }

    private LibvirtFtctlWrapperHelper() {
    }

    public static String getOutput(String result, OutputInterpreter.AllLinesParser parser) {
        return StringUtils.defaultIfBlank(parser.getLines(), StringUtils.defaultString(result));
    }

    public static JsonObject parseJsonObject(String output) {
        if (StringUtils.isBlank(output)) {
            return null;
        }
        JsonObject parsed = parseJsonObjectStrict(output.trim());
        if (parsed != null) {
            return parsed;
        }
        return parseLastJsonObject(output);
    }

    public static JsonObject parseSingleJsonObject(String output) {
        if (StringUtils.isBlank(output)) {
            return null;
        }
        String value = output.trim();
        if (!value.startsWith("{") || !value.endsWith("}")) {
            return null;
        }
        return parseJsonObjectStrict(value);
    }

    public static boolean isBooleanOrNull(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return true;
        }
        return object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isBoolean();
    }

    public static boolean isNumberOrNull(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return true;
        }
        return object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isNumber();
    }

    private static JsonObject parseJsonObjectStrict(String value) {
        try {
            JsonElement element = JsonParser.parseString(value);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private static JsonObject parseLastJsonObject(String output) {
        JsonObject lastObject = null;
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < output.length(); i++) {
            char ch = output.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
                continue;
            }
            if (ch == '}' && depth > 0) {
                depth--;
                if (depth == 0 && start >= 0) {
                    JsonObject candidate = parseJsonObjectStrict(output.substring(start, i + 1));
                    if (candidate != null) {
                        lastObject = candidate;
                    }
                    start = -1;
                }
            }
        }
        return lastObject;
    }

    public static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    public static Integer getInteger(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Long getLong(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Double getDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Boolean getBoolean(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            JsonElement element = object.get(key);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
