package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.metadata.MetaData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvMetaDataConsumerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesHeaderAndTrackDurations() throws IOException {
        Path csv = tempDir.resolve("tracks.csv");
        CsvMetaDataConsumer instance = new CsvMetaDataConsumer(csv, "Fallback");

        ZonedDateTime first = ZonedDateTime.parse("2026-03-14T10:00:00+01:00");
        ZonedDateTime second = ZonedDateTime.parse("2026-03-14T10:00:15+01:00");

        instance.accept(metaData(first, Optional.of("Station A"), Optional.of("Artist A"), Optional.of("Title A")));
        instance.setCurrentFilePath(tempDir.resolve("track-a.mp3"));
        instance.accept(metaData(second, Optional.of("Station A"), Optional.of("Artist B"), Optional.of("Title B")));
        instance.setCurrentFilePath(tempDir.resolve("track-b.mp3"));
        instance.close();

        List<String> lines = Files.readAllLines(csv);
        assertEquals(3, lines.size());
        assertEquals("start_time,duration_ms,station,artist,title,file_path", lines.get(0));
        assertEquals("2026-03-14T10:00:00+01:00,15000,Station A,Artist A,Title A," + tempDir.resolve("track-a.mp3"), lines.get(1));
        assertTrue(lines.get(2).startsWith("2026-03-14T10:00:15+01:00,"));
        assertTrue(lines.get(2).contains(",Station A,Artist B,Title B," + tempDir.resolve("track-b.mp3")));
    }

    @Test
    void usesFallbackStationName() throws IOException {
        Path csv = tempDir.resolve("tracks.csv");
        CsvMetaDataConsumer instance = new CsvMetaDataConsumer(csv, "Fallback Station");

        instance.accept(metaData(
                ZonedDateTime.now().minusSeconds(1),
                Optional.empty(),
                Optional.of("Artist"),
                Optional.of("Title")));
        instance.close();

        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains(",Fallback Station,Artist,Title,"));
    }

    @Test
    void escapesCsvColumns() throws IOException {
        Path csv = tempDir.resolve("tracks.csv");
        CsvMetaDataConsumer instance = new CsvMetaDataConsumer(csv, "Fallback");

        instance.accept(metaData(
                ZonedDateTime.now().minusSeconds(1),
                Optional.of("Station,One"),
                Optional.of("A \"B\""),
                Optional.of("Great, \"Song\"")));
        instance.close();

        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("\"Station,One\""));
        assertTrue(lines.get(1).contains("\"A \"\"B\"\"\""));
        assertTrue(lines.get(1).contains("\"Great, \"\"Song\"\"\""));
    }

    @Test
    void escapesCsvFilePath() throws IOException {
        Path csv = tempDir.resolve("tracks.csv");
        CsvMetaDataConsumer instance = new CsvMetaDataConsumer(csv, "Fallback");

        instance.setCurrentFilePath(tempDir.resolve("with,comma \"quote\".mp3"));
        instance.accept(metaData(
                ZonedDateTime.now().minusSeconds(1),
                Optional.of("Station"),
                Optional.of("Artist"),
                Optional.of("Title")));
        instance.close();

        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains(",\""));
        assertTrue(lines.get(1).contains("with,comma \"\"quote\"\".mp3"));
    }

    private static MetaData metaData(
            ZonedDateTime created,
            Optional<String> stationName,
            Optional<String> artist,
            Optional<String> title) {
        return MetaData.builder()
                .index(Optional.of(0))
                .created(created)
                .artist(artist)
                .title(title)
                .stationName(stationName)
                .stationUrl(Optional.empty())
                .offset(Optional.empty())
                .build();
    }
}
