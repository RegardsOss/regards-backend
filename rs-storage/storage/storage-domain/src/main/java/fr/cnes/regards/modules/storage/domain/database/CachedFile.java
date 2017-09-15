package fr.cnes.regards.modules.storage.domain.database;

import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CachedFile {

    private Long id;

    private String checksum;

    private URL location;

    private OffsetDateTime expiration;

    public CachedFile() {
    }

    public CachedFile(String checksum, URL location, OffsetDateTime expiration) {
        this.checksum = checksum;
        this.location = location;
        this.expiration = expiration;
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
}
