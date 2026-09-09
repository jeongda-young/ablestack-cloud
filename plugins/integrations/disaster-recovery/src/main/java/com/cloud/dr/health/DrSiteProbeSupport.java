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
package com.cloud.dr.health;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.nio.TrustAllManager;

public final class DrSiteProbeSupport {
    public static final String CLOUDSTACK_API_HMAC_ALGORITHM = "HmacSHA256";

    private DrSiteProbeSupport() {
    }

    public static String normalizeEndpoint(String endpoint, String defaultScheme) throws Exception {
        String normalized = StringUtils.trimToNull(endpoint);
        if (normalized == null) {
            return null;
        }
        if (!StringUtils.contains(normalized, "://")) {
            normalized = defaultScheme + "://" + normalized;
        }
        URL url = new URL(normalized);
        String path = StringUtils.defaultString(url.getPath());
        String query = url.getQuery();
        StringBuilder builder = new StringBuilder();
        builder.append(url.getProtocol()).append("://").append(url.getHost());
        if (url.getPort() > 0) {
            builder.append(":").append(url.getPort());
        }
        if (StringUtils.isNotBlank(path)) {
            builder.append(path);
        }
        if (StringUtils.isNotBlank(query)) {
            builder.append("?").append(query);
        }
        return builder.toString();
    }

    public static String normalizeRootEndpoint(String endpoint, String defaultScheme) throws Exception {
        String normalized = normalizeEndpoint(endpoint, defaultScheme);
        if (normalized == null) {
            return null;
        }
        URL url = new URL(normalized);
        StringBuilder builder = new StringBuilder();
        builder.append(url.getProtocol()).append("://").append(url.getHost());
        if (url.getPort() > 0) {
            builder.append(":").append(url.getPort());
        }
        return builder.toString();
    }

    public static String appendPath(String endpoint, String path) {
        String normalizedEndpoint = StringUtils.removeEnd(endpoint, "/");
        String normalizedPath = StringUtils.startsWith(path, "/") ? path : "/" + path;
        return normalizedEndpoint + normalizedPath;
    }

    public static HttpURLConnection openConnection(String url, String method, Boolean tlsVerify, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
        if (connection instanceof HttpsURLConnection && Boolean.FALSE.equals(tlsVerify)) {
            SSLContext sslContext = SSLUtils.getSSLContext();
            sslContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            HttpsURLConnection https = (HttpsURLConnection)connection;
            https.setSSLSocketFactory(sslContext.getSocketFactory());
            https.setHostnameVerifier((hostname, session) -> true);
        }
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setUseCaches(false);
        return connection;
    }

    public static String readBody(HttpURLConnection connection) {
        InputStream stream = null;
        try {
            stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return "";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String buildQuery(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> query = new ArrayList<String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            query.add(entry.getKey() + "=" + urlEncode(entry.getValue()));
        }
        return StringUtils.join(query, "&");
    }

    public static String signCloudStackRequest(Map<String, String> params, String secretKey) throws Exception {
        List<String> sortedParams = new ArrayList<String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            sortedParams.add(entry.getKey().toLowerCase() + "=" + urlEncode(entry.getValue()).toLowerCase());
        }
        Collections.sort(sortedParams);
        String request = StringUtils.join(sortedParams, "&");
        Mac mac = Mac.getInstance(CLOUDSTACK_API_HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), CLOUDSTACK_API_HMAC_ALGORITHM);
        mac.init(keySpec);
        return urlEncode(Base64.encodeBase64String(mac.doFinal(request.getBytes(StandardCharsets.UTF_8))));
    }

    public static String urlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
    }

    public static String basicAuth(String principal, String password) {
        String token = StringUtils.defaultString(principal) + ":" + StringUtils.defaultString(password);
        return "Basic " + Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
    }

    public static String fetchSha1Thumbprint(String endpoint, int timeoutMs) throws Exception {
        String normalized = normalizeRootEndpoint(endpoint, "https");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        URL url = new URL(normalized);
        String host = url.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        int port = url.getPort() > 0 ? url.getPort() : 443;
        SSLContext sslContext = SSLUtils.getSSLContext();
        sslContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
        try (SSLSocket socket = (SSLSocket)sslContext.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            socket.startHandshake();
            Certificate[] certificates = socket.getSession().getPeerCertificates();
            if (certificates == null || certificates.length == 0) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return formatFingerprint(digest.digest(certificates[0].getEncoded()));
        }
    }

    private static String formatFingerprint(byte[] digest) {
        if (digest == null || digest.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (byte value : digest) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(String.format("%02X", value & 0xff));
        }
        return builder.toString();
    }
}
