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

import de.sfuhrm.radiorecorder.metadata.MetaData;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes received metadata transitions to a CSV file.
 * Every row marks the start and duration of one encountered track.
 */
@Slf4j
class CsvMetaDataConsumer implements Consumer<MetaData>, AutoCloseable {

    private static final String HEADER = "start_time,duration_ms,station,artist,title,file_path";
    private static final Map<Path, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    private final Path csvOutputFile;
    private final String fallbackStationName;
    private final Object fileLock;

    private MetaData activeMetaData;
    private String activeFilePath = "";
    private boolean closed;

    CsvMetaDataConsumer(Path csvOutputFile, String fallbackStationName) {
        this.csvOutputFile = csvOutputFile.toAbsolutePath().normalize();
        this.fallbackStationName = fallbackStationName == null ? "" : fallbackStationName;
        this.fileLock = FILE_LOCKS.computeIfAbsent(this.csvOutputFile, p -> new Object());
    }

    void setCurrentFilePath(Path currentFilePath) {
        synchronized (this) {
            activeFilePath = currentFilePath == null
                    ? ""
                    : currentFilePath.toAbsolutePath().normalize().toString();
        }
    }

    @Override
    public void accept(MetaData currentMetaData) {
        MetaData localActiveMetaData;
        String localActiveFilePath;
        synchronized (this) {
            if (closed) {
                return;
            }
            localActiveMetaData = activeMetaData;
            localActiveFilePath = activeFilePath;
            activeMetaData = currentMetaData.clone();
        }
        if (localActiveMetaData != null) {
            appendTrack(localActiveMetaData, currentMetaData.getCreated(), localActiveFilePath);
        }
    }

    @Override
    public void close() {
        MetaData localActiveMetaData;
        String localActiveFilePath;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            localActiveMetaData = activeMetaData;
            localActiveFilePath = activeFilePath;
            activeMetaData = null;
            activeFilePath = "";
        }
        if (localActiveMetaData != null) {
            appendTrack(localActiveMetaData, ZonedDateTime.now(), localActiveFilePath);
        }
    }

    private void appendTrack(MetaData metaData, ZonedDateTime endExclusive, String filePath) {
        long durationMillis = Math.max(0, ChronoUnit.MILLIS.between(metaData.getCreated(), endExclusive));
        String station = metaData.getStationName().orElse(fallbackStationName);
        String artist = metaData.getArtist().orElse("");
        String title = metaData.getTitle().orElse("");
        String startTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(metaData.getCreated());

        String row = String.join(",",
                escape(startTime),
                Long.toString(durationMillis),
                escape(station),
                escape(artist),
                escape(title),
                escape(filePath)
        );
        appendLine(row);
    }

    private void appendLine(String line) {
        synchronized (fileLock) {
            try {
                Path parent = csvOutputFile.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                boolean writeHeader = !Files.exists(csvOutputFile) || Files.size(csvOutputFile) == 0;
                try (BufferedWriter writer = Files.newBufferedWriter(
                        csvOutputFile,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {
                    if (writeHeader) {
                        writer.write(HEADER);
                        writer.newLine();
                    }
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                log.warn("Could not append metadata CSV row to {}", csvOutputFile, e);
            }
        }
    }

    private static String escape(String input) {
        if (input == null) {
            return "";
        }
        boolean needsQuoting = input.indexOf(',') >= 0
                || input.indexOf('"') >= 0
                || input.indexOf('\n') >= 0
                || input.indexOf('\r') >= 0;
        String escaped = input.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
