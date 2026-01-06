package de.sfuhrm.radiorecorder.consumer;

import de.sfuhrm.radiorecorder.ConsumerContext;
import de.sfuhrm.radiorecorder.Radio;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import de.sfuhrm.radiorecorder.metadata.MimeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaDataFileNameGeneratorTest {

    @Mock
    private ConsumerContext consumerContext;

    @Mock
    private MetaData metaData;

    @Mock
    private Radio radio;

    private Path tmpDir;

    @BeforeEach
    void before() throws IOException {
        tmpDir = Files.createTempDirectory("rb");
        when(consumerContext.getTargetDirectory()).thenReturn(tmpDir);
    }

    void stubRadio() {
        when(radio.getName()).thenReturn("radiorecorderradio");
        when(radio.getUri()).thenReturn(URI.create("http://radiorecorder.com"));
    }

    @AfterEach
    void after() throws IOException {
        if (tmpDir != null) {
            try (Stream<Path> stream = Files.list(tmpDir)) {
                    stream.forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
                 Files.delete(tmpDir);
                 tmpDir = null;
        }
    }

    @Test
    public void getFileFromWithMetaDataAndContentTypeNull() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${id}${suffix}", consumerContext, false);
        Optional<Path> actual = instance.getFileFrom(radio, null, null);
        assertEquals(Optional.empty(), actual, "No content type means no path");
    }

    @Test
    public void getFileFromWithIdMetaDataNull() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${id}${suffix}", consumerContext, false);
        stubRadio();
        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("0.mp3")), actual, "Context id is used");
    }

    @Test
    public void getFileFromWithRadioNameMetaDataNull() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();
        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("radiorecorderradio.mp3")), actual, "Radio name is used");
    }

    @Test
    public void getFileFromWithTargetFileExists() throws IOException {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();
        Files.write(tmpDir.resolve("radiorecorderradio.mp3"), Collections.singletonList("hello"), StandardOpenOption.CREATE_NEW);
        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("radiorecorderradio-1.mp3")), actual, "Target file exists, generate new name");
    }

    @Test
    public void getFileFromWithReplacementFileExists() throws IOException {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();
        Files.write(tmpDir.resolve("radiorecorderradio.mp3"), Collections.singletonList("hello"), StandardOpenOption.CREATE_NEW);
        Files.write(tmpDir.resolve("radiorecorderradio-1.mp3"), Collections.singletonList("meow"), StandardOpenOption.CREATE_NEW);
        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("radiorecorderradio-2.mp3")), actual, "Target file exists, generate new name");
    }

    @Test
    public void getFileFromWithSanitizeVeryLong() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append('A');
        }
        when(radio.getName()).thenReturn(sb.toString());

        sb.setLength(0);
        for (int i = 0; i < 192; i++) {
            sb.append('A');
        }
        sb.append(".mp3");

        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve(sb.toString())), actual, "Long names get shortened to 192 chars");
    }

    @Test
    public void getFileFromWithSanitizeBeginning() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();

        when(radio.getName()).thenReturn(" -? Whitespace Radio");

        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("_Whitespace Radio.mp3")), actual, "Leading bad chars get replaced");
    }

    @Test
    public void getFileFromWithSanitizeInside() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${radioName}${suffix}", consumerContext, false);
        stubRadio();

        when(radio.getName()).thenReturn("Radio Smiley?:()/#");

        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("Radio Smiley______.mp3")), actual, "bad chars get replaced");
    }

    @Test
    public void getFileFromWithoutMetadataButRequiringIt() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${id}${suffix}", consumerContext, true);
        Optional<Path> actual = instance.getFileFrom(radio, null, MimeType.AUDIO_MPEG);
        assertEquals(Optional.empty(), actual, "Requires metadata, but didnt get it yet");
    }

    @Test
    public void getFileFromWithMetaData() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${artist} - ${title}${suffix}", consumerContext, false);
        stubRadio();
        when(metaData.getArtist()).thenReturn(Optional.of("Michael Jackson"));
        when(metaData.getTitle()).thenReturn(Optional.of("Bad"));
        Optional<Path> actual = instance.getFileFrom(radio, metaData, MimeType.AUDIO_MPEG);
        assertEquals(Optional.of(tmpDir.resolve("Michael Jackson - Bad.mp3")), actual, "Metadata used");
    }

    @Test
    public void getFileFromWithMetaDataWithIllegal() {
        MetaDataFileNameGenerator instance = new MetaDataFileNameGenerator("${artist} - ${title}${suffix}", consumerContext, false);
        stubRadio();
        when(metaData.getArtist()).thenReturn(Optional.of("fo\0x"));
        Optional<Path> actual = instance.getFileFrom(radio, metaData, MimeType.AUDIO_MPEG);

        // note: Null byte is only handled by the Linux OS as a unknown char
        String os = System.getProperty("os.name");
        if (os.equalsIgnoreCase("linux")) {
            assertEquals(Optional.of(tmpDir.resolve("fo_x - unknown.mp3")), actual, "Null byte is replaced with _");
        }
    }
}
