package de.sfuhrm.radiorecorder.aachelper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Provides classloaders loading the AAC classes for the {@link javax.sound.sampled.AudioSystem}.
 * These classes are only loaded on-demand if there's AAC content to play.
 * It's not advisable to regularly use these classes in the classpath since
 * the AAC plugin misinterprets MPEG to be AAC sometimes.
 *  */
@Slf4j
public class AACClassLoaderHelper {

    private static final String REMOTE_URL_AAC = "https://repo1.maven.org/maven2/com/tianscar/javasound/javasound-aac/0.9.8/javasound-aac-0.9.8.jar";
    private static final String REMOTE_URL_RES = "https://repo1.maven.org/maven2/com/tianscar/javasound/javasound-resloader/0.1.3/javasound-resloader-0.1.3.jar";

    private static final String LOCAL_JAR_AAC = "/lib/javasound-aac-0.9.8.jar";
    private static final String LOCAL_JAR_RES = "/lib/javasound-resloader-0.1.3.jar";

    /** Find the libs directory if running from a JAR. */
    static Optional<Path> findLibsDirectory() {
        try {
            URI uri = AACClassLoaderHelper.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path path = Paths.get(uri);
            if (Files.isRegularFile(path)) {
                log.debug("Path {} is regular file, we are in a jar", path);
                return Optional.of(path.getParent());
            }
        } catch (URISyntaxException e) {
            log.debug("Exception, but it is ok {}", e.getMessage());
            return Optional.empty();
        }

        return Optional.empty();
    }

    /** Get classloader loading the AAC classes from maven.org.
     * Just needed for IDE debugging.
     * */
    static ClassLoader loadPluginClassLoaderRemoteUrl() throws MalformedURLException {
        List<URL> urls = Arrays.asList(
                URI.create(REMOTE_URL_AAC).toURL(),
                URI.create(REMOTE_URL_RES).toURL()
        );
        URL[] urlArray = urls.toArray(new URL[0]);

        return new URLClassLoader(urlArray, AACClassLoaderHelper.class.getClassLoader());
    }

    /** Get classloader loading the AAC classes from maven.org.
     * Just needed for IDE debugging.
     * */
    static Optional<ClassLoader> loadPluginClassLoaderJarResources() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (String jar : Arrays.asList(LOCAL_JAR_AAC, LOCAL_JAR_RES)) {
            URL url = AACClassLoaderHelper.class.getResource(jar);
            if (url == null) {
                log.debug("Could not find jar {} in classpath", jar);
                return Optional.empty();
            }

            URL tempFileUrl = null;
            try {
                tempFileUrl = copyToTempFile(url);
            } catch (IOException e) {
                log.debug("Could not copy " + url.toExternalForm() + " to temp file", e);
                return Optional.empty();
            }

            urls.add(tempFileUrl);
        }
        log.debug("Found JARs: {}", urls);
        URL[] urlArray = urls.toArray(new URL[0]);
        return Optional.of(new URLClassLoader(urlArray, AACClassLoaderHelper.class.getClassLoader()));
    }

    /** Copies the input jarUrl to a new tempfile. */
    private static URL copyToTempFile(URL jarUrl) throws IOException {
        Path tempFile = Files.createTempFile("radiorecorder", ".jar");
        try (OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
             InputStream inputStream = jarUrl.openStream()) {
            inputStream.transferTo(outputStream);
            return tempFile.toUri().toURL();
        }
    }


    public static ClassLoader loadPluginClassLoader() throws MalformedURLException {
        ClassLoader result;

        Optional<ClassLoader> jarResourceClassLoader = loadPluginClassLoaderJarResources();
        if (jarResourceClassLoader.isPresent()) {
            result = jarResourceClassLoader.get();
        } else {
            log.warn("Using remote JARs, should only be used for development!");
            result = loadPluginClassLoaderRemoteUrl();
        }

        return result;
    }
}