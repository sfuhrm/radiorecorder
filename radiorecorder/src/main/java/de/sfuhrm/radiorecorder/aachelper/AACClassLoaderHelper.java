package de.sfuhrm.radiorecorder.aachelper;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String URL_AAC = "https://repo1.maven.org/maven2/com/tianscar/javasound/javasound-aac/0.9.8/javasound-aac-0.9.8.jar";
    private static final String URL_RES = "https://repo1.maven.org/maven2/com/tianscar/javasound/javasound-resloader/0.1.3/javasound-resloader-0.1.3.jar";

    private static final String JAR_AAC = "javasound-aac-0.9.8.jar";
    private static final String JAR_RES = "javasound-resloader-0.1.3.jar";

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
                URI.create(URL_AAC).toURL(),
                URI.create(URL_RES).toURL()
        );
        URL[] urlArray = urls.toArray(new URL[0]);

        return new URLClassLoader(urlArray, AACClassLoaderHelper.class.getClassLoader());
    }

    /** Get classloader loading the AAC classes from maven.org.
     * Just needed for IDE debugging.
     * */
    static Optional<ClassLoader> loadPluginClassLoaderPluginDir() throws MalformedURLException {
        Optional<Path> libDirPath = findLibsDirectory();

        if (libDirPath.isPresent()) {
            // resolve provided
            Path libDir = libDirPath.get();
            Path providedDir = libDir.resolve("provided");
            if (Files.isDirectory(providedDir)) {
                List<URL> urls = new ArrayList<>();
                for (String jar : Arrays.asList(JAR_AAC, JAR_RES)) {
                    Path jarPath = providedDir.resolve(jar);
                    if (! Files.isRegularFile(jarPath)) {
                        log.debug("Could not find jar {} in path {}", jar, providedDir);
                        return Optional.empty();
                    }

                    urls.add(jarPath.toUri().toURL());
                }
                log.debug("Found JARs: {}", urls);
                URL[] urlArray = urls.toArray(new URL[0]);
                return Optional.of(new URLClassLoader(urlArray, AACClassLoaderHelper.class.getClassLoader()));
            } else {
                log.debug("Provided does not exist");
            }
        } else {
            log.debug("Lib dir path does not exist");
        }

        return Optional.empty();
    }


    public static ClassLoader loadPluginClassLoader() throws MalformedURLException {
        ClassLoader result;

        Optional<ClassLoader> pluginDirClassLoader = loadPluginClassLoaderPluginDir();
        if (pluginDirClassLoader.isPresent()) {
            result = pluginDirClassLoader.get();
        } else {
            log.warn("Using remote JARs, should only be used for development!");
            result = loadPluginClassLoaderRemoteUrl();
        }

        return result;
    }
}