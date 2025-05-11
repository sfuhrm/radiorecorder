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
package de.sfuhrm.radiorecorder.aachelper;

import org.junit.jupiter.api.Test;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Test for {@link AACClassLoaderHelper}.
 * @author Stephan Fuhrmann
 */
public class AACClassLoaderHelperTest {

    @Test
    public void findLibsDirectory() {
        Optional<Path> dir = AACClassLoaderHelper.findLibsDirectory();
        assertFalse(dir.isPresent(), "Should always be false, we are building and testing from classpath dir, not jars");
    }

    @Test
    public void loadPluginClassLoaderRemoteUrl() throws MalformedURLException, ClassNotFoundException {
        ClassLoader loader = AACClassLoaderHelper.loadPluginClassLoaderRemoteUrl();
        Class<?> clazz = loader.loadClass("net.sourceforge.jaad.spi.javasound.AACAudioFileReader");
        assertNotNull(clazz, "Internet loading JARs should always work");
    }

    @Test
    public void loadPluginClassLoaderJarResources() throws MalformedURLException, ClassNotFoundException {
        Optional<ClassLoader> loader = AACClassLoaderHelper.loadPluginClassLoaderJarResources();
        assertTrue(loader.isPresent(), "Should always be false, we are building and testing from classpath dir, not jars");
        Class<?> clazz = loader.get().loadClass("net.sourceforge.jaad.spi.javasound.AACAudioFileReader");
        assertNotNull(clazz, "Local loading JARs should always work");
    }

    @Test
    public void loadPluginClassLoader() throws MalformedURLException, ClassNotFoundException {
        ClassLoader loader = AACClassLoaderHelper.loadPluginClassLoader();
        Class<?> clazz = loader.loadClass("net.sourceforge.jaad.spi.javasound.AACAudioFileReader");
        assertNotNull(clazz, "Should have the internet jars");
    }
}
