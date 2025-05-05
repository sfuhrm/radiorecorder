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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Temporary file reference that will delete on closing.
 * @author Stephan Fuhrmann
 */
@Slf4j
@Getter
public class TemporaryFile implements AutoCloseable {
    private final Path file;

    public TemporaryFile() throws IOException {
        file = Files.createTempFile("stfu", "tmp");
    }

    public void write(String text, Charset charset) throws IOException {
        byte[] data = text.getBytes(charset);
        Files.write(file, data);
    }

    public void write(String data) throws IOException {
        write(data, StandardCharsets.UTF_8);
    }

    public URL getURL() throws MalformedURLException {
        return file.toUri().toURL();
    }

    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(file);
    }

    @Override
    public void close() {
        try {
            Files.delete(file);
        } catch (IOException e) {
            log.warn("Could not delete {}", file);
        }
    }
}
