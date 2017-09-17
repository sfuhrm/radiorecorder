/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.nio.file.Files;
import lombok.Getter;

/**
 * Temporary file reference that will delete on closing.
 * @author Stephan Fuhrmann
 */
public class TemporaryFile implements AutoCloseable {
    @Getter
    private final File file;

    public TemporaryFile() throws IOException {
        file = File.createTempFile("stfu", "tmp");
    }
    
    public void write(String text, Charset charset) throws IOException {
        byte[] data = text.getBytes(charset);
        Files.write(file.toPath(), data);
    }
    
    public void write(String data) throws IOException {
        write(data, Charset.forName("UTF-8"));
    }
    
    public URL getURL() throws MalformedURLException {
        return file.toURI().toURL();
    }
    
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public void close() {
        file.delete();
    }
}
