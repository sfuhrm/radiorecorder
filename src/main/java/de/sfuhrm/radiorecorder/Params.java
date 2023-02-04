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
package de.sfuhrm.radiorecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import de.sfuhrm.radiorecorder.http.HttpConnectionBuilderFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * The command line parameters as a POJO.
 * Must be created using {@link #parse(java.lang.String[]) }.
 * @see #parse(java.lang.String[])
 * @author Stephan Fuhrmann
 */
@Slf4j
public class Params {

    @Getter
    @Option(name = "-help", aliases = {"-h"}, usage = "Show this command line help.", help = true)
    private boolean help;

    @Getter
    @Option(name = "-directory", aliases = {"-d"}, usage = "Write recorded stream files to a folder hierarchy in this target directory.", metaVar = "DIR")
    private File directory;

    @Getter
    @Option(name = "-use-songnames", aliases = {"-S"}, usage = "Use songnames from retrieved metadata information. Will create one file per detected song.")
    private boolean songNames;

    @Getter
    @Option(name = "-limit", aliases = {"-l"}, usage = "Limit of stations to download in parallel.", metaVar = "COUNT")
    private int stationLimit = 10;

    @Getter
    @Option(name = "-min-free", aliases = {"-M"}, usage = "Minimum of free megs on target drive.", metaVar = "MEGS")
    private long minimumFreeMegs = 512;

    @Getter
    @Option(name = "-abort-after", usage = "Abort after writing the given amount of kilobytes to target drive.", metaVar = "KB")
    private Long abortAfterKilo;

    @Getter
    @Option(name = "-reconnect", aliases = {"-r"}, usage = "Automatically reconnect after connection loss.")
    private boolean reconnect;

    @Getter
    @Option(name = "-play", aliases = {"-p"}, usage = "Play live instead of recording to a file.")
    private boolean play;

    @Getter
    @Option(name = "-version", aliases = {"-V"}, usage = "Show version information and exit.", help = true)
    private boolean version;

    @Getter
    @Option(name = "-mixer", aliases = {"-m"}, usage = "The mixer to use for playback. " +
            "The mixer parameter is the name from the '-list-mixer' option output.",
            metaVar = "MIXER_NAME")
    private String mixer;

    @Getter
    @Option(name = "-list-mixer", aliases = {"-X"}, usage = "List audio playback mixers, then exit.", help = true)
    private boolean listMixers;

    @Getter
    @Option(name = "-list-cast", aliases = {"-L"}, usage = "List chromecast devices, then exit.", help = true)
    private boolean listCast;

    @Getter
    @Option(name = "-list-station", aliases = {"-Z"}, usage = "List matching radio stations limited by '-limit', then exit.", help = true)
    private boolean listStation;

    @Getter
    @Option(name = "-cast", aliases = {"-c"}, usage = "Stream to the given chrome cast device. Use cast device title from '-list-cast'.",
            metaVar = "CASTDEVICE_TITLE")
    private String castReceiver;

    @Getter
    @Option(name = "-timeout", aliases = {"-T"}, usage = "Connect/read timeout in seconds.", metaVar = "SECS")
    private int timeout = 60;

    @Getter
    @Option(name = "-client", aliases = {"-C"}, usage = "Specify HTTP client to use.", metaVar = "CLIENT")
    private HttpConnectionBuilderFactory.HttpClientType httpClientType = HttpConnectionBuilderFactory.HttpClientType.APACHE_CLIENT_5;

    @Getter
    @Option(name = "-proxy", aliases = {"-P"}, usage = "The HTTP/HTTPS proxy to use.", metaVar = "URL")
    private URL proxy;

    @Getter
    @Argument(usage = "URLs of the internet radio station(s), (partial) station name for lookup or the station " +
            "UUID (see option -list-station)", metaVar = "URL_OR_UUID_OR_NAME", required = true)
    private List<String> arguments;

    /** Parse the command line options.
     * @param args the command line args as passed to the main method of the
     * program.
     * @return the parsed command line options or {@code null} if
     * the program needs to exit. {@code null} will be returned
     * if the command lines are wrong or the command line help
     * was displayed.
     */
    public static Params parse(String[] args) {
        CmdLineParser cmdLineParser = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Args: {}", Arrays.toString(args));
            }

            Params result = new Params();

            cmdLineParser = new CmdLineParser(result);
            cmdLineParser.parseArgument(args);

            if (result.help) {
                cmdLineParser.printUsage(System.err);
                return null;
            }

            if (result.isVersion()) {
                result.showVersion();
                return null;
            }

            boolean isList = result.isListCast() || result.isListStation() || result.isListMixers();

            if (!result.isPlay() && !isList && result.getDirectory() == null) {
                cmdLineParser.printUsage(System.err);
                log.error("Not playing, need a target directory (-directory)!");
                return null;
            }

            if (result.getDirectory() != null
                && ! Files.isWritable(result.getDirectory().toPath())) {
                cmdLineParser.printUsage(System.err);
                log.error("Target directory {} given, but it is not writable!",
                        result.getDirectory());
                return null;
            }

            return result;
        } catch (CmdLineException ex) {
            log.warn("Error in parsing", ex);
            cmdLineParser.printUsage(System.err);
        } catch (IOException e) {
            log.error("Error in program", e);
        }
        return null;
    }

    private void showVersion() throws IOException {
        try (InputStream inputStream = Main.class.getResourceAsStream("/application.properties")) {
            Properties applicationProperties = new Properties();
            if (inputStream != null) {
                applicationProperties.load(inputStream);
            }
            String unknown = "unknown";
            System.err.printf("Application:  %s%n", applicationProperties.getOrDefault("application.name", unknown));
            System.err.printf("Version:      %s%n", applicationProperties.getOrDefault("application.version", unknown));
            System.err.printf("Build date:   %s%n", applicationProperties.getOrDefault("build.timestamp", unknown));
        }
    }
}
