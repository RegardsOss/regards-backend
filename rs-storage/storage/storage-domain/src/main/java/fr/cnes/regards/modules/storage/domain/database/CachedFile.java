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

    /**
     * unique business id
     */
    @Column(length = 512)
    private String checksum;

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

    @Column
    @Enumerated(EnumType.STRING)
    private CachedFileState state;

    @Column(length = 512)
    private String failureCause;

    public CachedFile() {
    }

    public CachedFile(DataFile df, OffsetDateTime expiration) {
        this.checksum = df.getChecksum();
        this.expiration = expiration;
        this.state = CachedFileState.RESTORING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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
}
