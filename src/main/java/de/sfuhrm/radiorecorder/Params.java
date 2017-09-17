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
import java.util.Arrays;
import java.util.List;
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
    @Option(name = "-directory", aliases = {"-d"}, usage = "Write to this directory.", metaVar = "DIR")
    private File directory;
    
    @Getter
    @Option(name = "-use-songnames", aliases = {"-S"}, usage = "Use songnames from retrieved metadata information. Will create one file per detected song.")
    private boolean songNames;
    
    @Getter
    @Option(name = "-min-free", aliases = {"-M"}, usage = "Minimum of free megs on target drive.", metaVar = "MEGS")
    private long minimumFree = 512;
    
    @Getter
    @Option(name = "-reconnect", aliases = {"-r"}, usage = "Automatically reconnect after connection loss.")
    private boolean reconnect;
    
    @Getter
    @Option(name = "-play", aliases = {"-p"}, usage = "Play live instead of recording to a file.")
    private boolean play;
    
    @Getter
    @Option(name = "-timeout", aliases = {"-T"}, usage = "Connect/read timeout in seconds.", metaVar = "SECS")
    private int timeout = 60;
    
      
    @Getter
    @Argument
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
            result.directory = new File(System.getProperty("user.home"));
            
            cmdLineParser = new CmdLineParser(result);
            cmdLineParser.parseArgument(args);
            
            if (result.help) {
                cmdLineParser.printUsage(System.err);
                return null;
            }
                        
            return result;
        } catch (CmdLineException ex) {
            log.warn("Error in parsing", ex);
            System.err.println(ex.getMessage());
            if (cmdLineParser != null) {
                cmdLineParser.printUsage(System.err);
            }
        }
        return null;
    }
}
