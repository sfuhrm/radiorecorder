package de.sfuhrm.radiorecorder.consumer;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import de.sfuhrm.radiorecorder.Main;
import de.sfuhrm.radiorecorder.metadata.MetaData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class ID3Tagger implements Runnable {
    private MetaData metaData;
    private Path file;

    public ID3Tagger(@NonNull final MetaData md, @NonNull final Path file) {
        this.metaData = md;
        this.file = file;
    }

    @Override
    public void run() {
        try {
            log.debug("Adding id3 tag to {}", file);
            Mp3File mp3File = new Mp3File(file);

            ID3v1 id3v1 = new ID3v1Tag();
            metaData.getTitle().ifPresent(id3v1::setTitle);
            metaData.getArtist().ifPresent(id3v1::setArtist);
            metaData.getStationName().ifPresent(id3v1::setComment);
            mp3File.setId3v1Tag(id3v1);

            ID3v24Tag id3v2 = new ID3v24Tag();
            metaData.getTitle().ifPresent(id3v2::setTitle);
            metaData.getArtist().ifPresent(id3v2::setArtist);
            metaData.getStationName().ifPresent(id3v2::setPublisher);
            metaData.getStationUrl().ifPresent(id3v2::setRadiostationUrl);
            id3v2.setComment(Main.PROJECT);
            id3v2.setUrl(Main.GITHUB_URL);
            mp3File.setId3v2Tag(id3v2);

            Path bak = file.getParent().resolve(file.getFileName() + ".bak");
            Path tmp = file.getParent().resolve(file.getFileName() + ".tmp");
            mp3File.save(tmp.toFile().getAbsolutePath());

            // foo.mp3 -> foo.mp3.bak
            Files.move(file, bak);
            // foo.mp3.tmp -> foo.mp3
            Files.move(tmp, file);
            // foo.mp3.bak
            Files.delete(bak);

            log.debug("Done adding id3 tag to {}", file);
        } catch (NotSupportedException | UnsupportedTagException | InvalidDataException | IOException ex) {
            log.warn("Exception while writing id3 tag for " + file, ex);
        }
    }
}