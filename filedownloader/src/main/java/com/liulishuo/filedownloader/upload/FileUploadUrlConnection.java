/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.upload;

import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUploadUrlConnection implements FileDownloadConnection {
    private final Map<String, List<String>> _respHeaders = new HashMap<>();
    private final Map<String, List<String>> _reqHeaders = new HashMap<>();

    public FileUploadUrlConnection(URL url, FileDownloadUrlConnection.Configuration configuration) throws IOException {
    }

    public FileUploadUrlConnection(String originUrl, FileDownloadUrlConnection.Configuration configuration)
        throws IOException {
    }

    public FileUploadUrlConnection(String originUrl) throws IOException {
        this(originUrl, null);
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public boolean dispatchAddResumeOffset(String etag, long offset) {
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestHeaderFields() {
        return _reqHeaders;
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        return _respHeaders;
    }

    @Override
    public String getResponseHeaderField(String name) {
        return null;
    }

    @Override
    public boolean setRequestMethod(String method) throws ProtocolException {
        return false;
    }

    @Override
    public void execute() throws IOException {

    }

    @Override
    public int getResponseCode() throws IOException {
        return 0;
    }

    @Override
    public void ending() {

    }
}
