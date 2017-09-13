package fr.cnes.regards.modules.order.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.net.URI;
import java.nio.file.FileSystems;

import org.springframework.util.MimeType;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.urn.converters.UrnConverter;
import fr.cnes.regards.modules.indexer.domain.DataFile;

/**
 * Inherits from DataFile to add nearline state and IP_ID
 * @author oroussel
 */
@Entity
@Table(name = "t_data_file")
public class OrderDataFile extends DataFile {

    private FileState state;

    /**
     * DataObject IP_ID
     */
    private UniformResourceName ipId;

    public void setState(FileState state) {
        this.state = state;
    }

    public OrderDataFile() {
    }

    public OrderDataFile(DataFile dataFile, UniformResourceName ipId) {
        setName(dataFile.getName());
        setSize(dataFile.getSize());
        setUri(dataFile.getUri());
        setChecksum(dataFile.getChecksum());
        setDigestAlgorithm(dataFile.getDigestAlgorithm());
        setMimeType(dataFile.getMimeType());
        setState(FileState.PENDING);
        setOnline(dataFile.getOnline());
        setIpId(ipId);
    }

    @Column(name = "data_objects_ip_id", )
    @Convert(converter = UrnConverter.class)
    public UniformResourceName getIpId() {
        return ipId;
    }

    public FileState getState() {
        return state;
    }

    @Override
    @Column(name = "uri", columnDefinition = "text")
    public URI getUri() {
        return super.getUri();
    }

    @Override
    @Column(name = "checksum_algo", length = 10)
    public String getDigestAlgorithm() {
        return super.getDigestAlgorithm();
    }

    @Override
    @Column(name = "checksum", length = 128)
    public String getChecksum() {
        return super.getChecksum();
    }

    @Override
    @Column(name = "size")
    public Long getSize() {
        return super.getSize();
    }

    @Override
    @Column(name = "mime_type", length = 64) // See RFC 6838
    public MimeType getMimeType() {
        return super.getMimeType();
    }

    @Override
    @Column(name = "name", length = 255)
    public String getName() {
        return super.getName();
    }

    @Override
    @Column(name = "online")
    public Boolean getOnline() {
        return super.getOnline();
    }


    public void setIpId(UniformResourceName ipId) {
        this.ipId = ipId;
    }

    @Override
    public void setOnline(Boolean online) {
        super.setOnline(online);
        if (online) {
            this.state = FileState.ONLINE;
        }
    }
}
