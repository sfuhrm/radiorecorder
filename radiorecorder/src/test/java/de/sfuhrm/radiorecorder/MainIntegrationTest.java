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

import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainIntegrationTest {

    @Test
    void testlistStation() throws IOException, InterruptedException {
        Main.main(new String[] {"-Z", "synth"});
    }


    @Test
    void testAbortAfterDuration() throws IOException, InterruptedException {
        Path tmpDir = Files.createTempDirectory("rb");
        try {
            Main.main(new String[]{"synth", "-abort-after-duration", "3s", "-limit", "1", "-d", tmpDir.toAbsolutePath().toString()});

            // expecting one file
            List<Path> files = listRecursively(tmpDir);
            // one file
            assertEquals(1, files.stream().filter(Files::isRegularFile).count());
            // non zero file
            assertThat(Files.size(files.get(0)), IsNot.not(0));
            // mp3 (might be unstable test???)
            assertThat(files.get(0).toString(), StringEndsWith.endsWith(".mp3"));
        }
        finally {
            deleteRecursively(tmpDir);
        }
    }

    private static List<Path> listRecursively(Path p) throws IOException {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> pathStream = Files.list(p)) {
            pathStream.forEach(entry -> {
                if (Files.isDirectory(entry)) {
                    try {
                        result.addAll(listRecursively(entry));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (Files.isRegularFile(entry)) {
                    result.add(entry);
                }
            });
        }
        return result;
    }

    private static void deleteRecursively(Path p) throws IOException {
        try (Stream<Path> pathStream = Files.list(p)) {
            pathStream.forEach(entry -> {
                if (Files.isDirectory(entry)) {
                    try {
                        deleteRecursively(entry);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Files.delete(entry);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
