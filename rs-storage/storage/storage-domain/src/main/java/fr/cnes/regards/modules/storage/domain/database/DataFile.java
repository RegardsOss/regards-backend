package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.*;
import java.net.URL;

import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.FileType;

/**
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_data_file")
public class DataFile {

    @Id
    @SequenceGenerator(name = "dataFileSequence", initialValue = 1, sequenceName = "seq_data_file")
    @GeneratedValue(generator = "dataFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private URL originUrl;

    @Column(length = AIPDataBase.CHECKSUM_MAX_LENGTH, nullable = false)
    private String checksum;

    @Column(nullable = false)
    private String algorithm;

    @Column
    @Enumerated(EnumType.STRING)
    private FileType type;

    @Column
    private Double fileSize;

    @Column(nullable = false)
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_storage_plugin_configuration", foreignKey = @ForeignKey(name = "fk_data_file_data_storage_plugin_configuration"))
    private PluginConfiguration dataStorageUsed;

    public DataFile() {
    }

    public DataFile(DataObject file, String algorithm, String checksum, Double fileSize, MimeType mimeType) {
        this.algorithm=algorithm;
        this.checksum=checksum;
        this.fileSize=fileSize;
        this.originUrl=file.getUrl();
        this.type=file.getType();
        this.mimeType=mimeType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(URL originUrl) {
        this.originUrl = originUrl;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public Double getFileSize() {
        return fileSize;
    }

    public void setFileSize(Double fileSize) {
        this.fileSize = fileSize;
    }

    public PluginConfiguration getDataStorageUsed() {
        return dataStorageUsed;
    }

    public void setDataStorageUsed(PluginConfiguration dataStorageUsed) {
        this.dataStorageUsed = dataStorageUsed;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }
}
