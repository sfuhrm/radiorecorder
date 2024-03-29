/*
 * Copyright 2017 Stephan Fuhrmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sfuhrm.radiorecorder.consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import lombok.Getter;

/**
 * Temporary file reference that will delete on closing.
 * @author Stephan Fuhrmann
 */
@Getter
public class TemporaryFile implements AutoCloseable {
    private final File file;

    public TemporaryFile() throws IOException {
        file = File.createTempFile("stfu", "tmp");
    }

    public void write(String text, Charset charset) throws IOException {
        byte[] data = text.getBytes(charset);
        Files.write(file.toPath(), data);
    }

    public void write(String data) throws IOException {
        write(data, StandardCharsets.UTF_8);
    }

    public URL getURL() throws MalformedURLException {
        return file.toURI().toURL();
    }

    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(file.toPath());
    }

    @Override
    public void close() {
        file.delete();
    }
}
