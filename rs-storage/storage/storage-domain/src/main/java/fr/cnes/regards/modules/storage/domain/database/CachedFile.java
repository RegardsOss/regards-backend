package fr.cnes.regards.modules.storage.domain.database;

import java.net.URL;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_cached_file")
public class CachedFile {

    /**
     * db id
     */
    @Id
    @SequenceGenerator(name = "cachedFileSequence", initialValue = 1, sequenceName = "seq_cached_file")
    @GeneratedValue(generator = "cachedFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String checksum;

    @Column
    private Long fileSize;

    /**
     * location into the cache
     */
    @Column
    private URL location;

    /**
     * expiration date of the file into the cache
     */
    @Column
    private OffsetDateTime expiration;

    /**
     * Date of the last request to make the file available.
     */
    @Column
    private OffsetDateTime lastRequestDate;

    @Column
    @Enumerated(EnumType.STRING)
    private CachedFileState state;

    @Column(length = 512)
    private String failureCause;

    public CachedFile() {
    }

    public CachedFile(DataFile df, OffsetDateTime expirationDate, CachedFileState fileState) {
        checksum = df.getChecksum();
        fileSize = df.getFileSize();
        expiration = expirationDate;
        lastRequestDate = OffsetDateTime.now();
        state = fileState;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URL getLocation() {
        return location;
    }

    public void setLocation(URL location) {
        this.location = location;
    }

    public OffsetDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(OffsetDateTime expiration) {
        this.expiration = expiration;
    }

    public CachedFileState getState() {
        return state;
    }

    public void setState(CachedFileState state) {
        this.state = state;
    }

    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }

    public String getFailureCause() {
        return failureCause;
    }

    public OffsetDateTime getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(OffsetDateTime pLastRequestDate) {
        lastRequestDate = pLastRequestDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long pFileSize) {
        fileSize = pFileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

}
