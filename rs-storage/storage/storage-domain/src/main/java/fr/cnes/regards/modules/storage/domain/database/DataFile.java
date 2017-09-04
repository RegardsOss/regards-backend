package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.*;
import java.net.URL;
import java.util.Set;

import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.domain.InformationObject;

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
    private DataType type;

    @Column
    private Long fileSize;

    @Column
    private DataFileState state;

    @Column(nullable = false)
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_storage_plugin_configuration",
            foreignKey = @ForeignKey(name = "fk_data_file_data_storage_plugin_configuration"))
    private PluginConfiguration dataStorageUsed;

    /**
     * Reversed mapping compared to reality. This is because it is easier to work like this.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aip_ip_id", foreignKey = @ForeignKey(name = "fk_aip_data_file"))
    private AIPDataBase aipDataBase;

    public DataFile(DataObject file, String algorithm, String checksum, Long fileSize, MimeType mimeType, AIP aip) {
        this(file.getUrl(), checksum, algorithm, file.getType(), fileSize, mimeType, aip);
    }

    public DataFile(URL originUrl, String checksum, String algorithm, DataType type, Long fileSize, MimeType mimeType,
            AIP aip) {
        this.originUrl = originUrl;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.type = type;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.aipDataBase = new AIPDataBase(aip);
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

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
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

    public DataFileState getState() {
        return state;
    }

    public void setState(DataFileState state) {
        this.state = state;
    }

    public void setAipDataBase(AIPDataBase aipDataBase) {
        this.aipDataBase = aipDataBase;
    }

    public AIPDataBase getAipDataBase(AIPDataBase aipDataBase) {
        return this.aipDataBase;
    }

    public AIP getAip() {
        return aipDataBase.getAip();
    }

    public void setAip(AIP aip) {
        this.aipDataBase = new AIPDataBase(aip);
    }

    public static Set<DataFile> extractDataFiles(AIP aip) {
        Set<DataFile> dataFiles = Sets.newHashSet();
        for (InformationObject io : aip.getInformationObjects()) {
            DataObject file = io.getContentInformation().getDataObject();
            MimeType mimeType = MimeType
                    .valueOf(io.getContentInformation().getRepresentationInformation().getSyntax().getMimeType());
            String algorithm = io.getPdi().getFixityInformation().getAlgorithm();
            String checksum = io.getPdi().getFixityInformation().getChecksum();
            Long fileSize = io.getPdi().getFixityInformation().getFileSize();
            dataFiles.add(new DataFile(file, algorithm, checksum, fileSize, mimeType, aip));
        }
        return dataFiles;
    }
}
