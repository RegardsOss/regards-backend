package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.net.URL;
import java.util.Set;

import org.springframework.util.MimeType;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 *
 * contains useful, for the system, metadata of a file from an AIP.
 * It mirrors {@link OAISDataObject} and add some information needed by the system not extracted from the AIP
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_data_file", indexes = { @Index(name = "idx_data_file_checksum", columnList = "checksum") })
@NamedEntityGraph(name = "graph.datafile.full", attributeNodes = { @NamedAttributeNode("aipEntity"),
        @NamedAttributeNode(value = "dataStorageUsed", subgraph = "graph.datafile.dataStorageUsed") }, subgraphs = {
        @NamedSubgraph(name = "graph.datafile.dataStorageUsed", attributeNodes = {
                @NamedAttributeNode(value = "parameters", subgraph = "graph.datafile.dataStorageUsed.parameters") }),
        @NamedSubgraph(name = "graph.datafile.dataStorageUsed.parameters",
                attributeNodes = { @NamedAttributeNode("dynamicsValues") }) })
public class StorageDataFile {

    /**
     * length used as the checksum column definition. Why 128? it allows to use sha-512. That should limit issues with checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    /**
     * The id
     */
    @Id
    @SequenceGenerator(name = "dataFileSequence", initialValue = 1, sequenceName = "seq_data_file")
    @GeneratedValue(generator = "dataFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * File url
     */
    @Column
    private URL url;

    /**
     * File name
     */
    @Column
    private String name;

    /**
     * Checksum
     */
    @Column(length = CHECKSUM_MAX_LENGTH, nullable = false)
    private String checksum;

    /**
     * Checksum algorithm
     */
    @Column(nullable = false)
    private String algorithm;

    /**
     * Data type
     */
    @Column
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    /**
     * File size
     */
    @Column
    private Long fileSize;

    /**
     * File state
     */
    @Column
    @Enumerated(EnumType.STRING)
    private DataFileState state;

    /**
     * File mime type
     */
    @Column(nullable = false)
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    @Column
    private int height;

    @Column
    private int width;

    /**
     * Data storage plugin configuration used to store the file
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_storage_plugin_configuration",
            foreignKey = @ForeignKey(name = "fk_data_file_data_storage_plugin_configuration"))
    private PluginConfiguration dataStorageUsed;

    /**
     * Reversed mapping compared to reality. This is because it is easier to work like this.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aip_ip_id", foreignKey = @ForeignKey(name = "fk_aip_data_file"))
    @GsonIgnore
    private AIPEntity aipEntity;

    /**
     * Default constructor
     */
    private StorageDataFile() {
        // serialization
    }

    /**
     * Initialize the data file from the parameters
     * @param file
     * @param mimeType
     * @param aip
     */
    public StorageDataFile(OAISDataObject file, MimeType mimeType, AIP aip) {
        this(file.getUrl(),
             file.getChecksum(),
             file.getAlgorithm(),
             file.getRegardsDataType(),
             file.getFileSize(),
             mimeType,
             aip,
             null);
        String name = file.getFilename();
        if (Strings.isNullOrEmpty(name)) {
            String[] pathParts = file.getUrl().getPath().split("/");
            name = pathParts[pathParts.length - 1];
        }
        this.name = name;
    }

    /**
     * Constructor setting the parameters as attribute except for the aip which is transformed to a {@link AIPEntity}
     * @param url
     * @param checksum
     * @param algorithm
     * @param type
     * @param fileSize
     * @param mimeType
     * @param aip
     * @param name
     */
    public StorageDataFile(URL url, String checksum, String algorithm, DataType type, Long fileSize, MimeType mimeType,
            AIP aip, String name) {
        this.url = url;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.dataType = type;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.aipEntity = new AIPEntity(aip);
        this.name = name;
    }

    /**
     * Extract the different file metadata from an aip
     * @param aip
     * @return extracted data files
     */
    public static Set<StorageDataFile> extractDataFiles(AIP aip) {
        Set<StorageDataFile> dataFiles = Sets.newHashSet();
        for (ContentInformation ci : aip.getProperties().getContentInformations()) {
            OAISDataObject file = ci.getDataObject();
            MimeType mimeType = MimeType.valueOf(ci.getRepresentationInformation().getSyntax().getMimeType());
            dataFiles.add(new StorageDataFile(file, mimeType, aip));
        }
        return dataFiles;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the id
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Set the url
     * @param url
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     * @param checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the checksum algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the checksum algorithm
     * @param algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the data type
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Set the data type
     * @param type
     */
    public void setDataType(DataType type) {
        this.dataType = type;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     * @param fileSize
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the data storage plugin configuration
     */
    public PluginConfiguration getDataStorageUsed() {
        return dataStorageUsed;
    }

    /**
     * Set the data storage plugin configuration
     * @param dataStorageUsed
     */
    public void setDataStorageUsed(PluginConfiguration dataStorageUsed) {
        this.dataStorageUsed = dataStorageUsed;
    }

    /**
     * @return the mime type
     */
    public MimeType getMimeType() {
        return mimeType;
    }

    /**
     * Set the mime type
     * @param mimeType
     */
    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the state
     */
    public DataFileState getState() {
        return state;
    }

    /**
     * Set the state
     * @param state
     */
    public void setState(DataFileState state) {
        this.state = state;
    }

    /**
     * @return the jpa representation of the associated aip(AIPEntity)
     */
    public AIPEntity getAipEntity() {
        return this.aipEntity;
    }

    /**
     * Set the jpa representation fo the associated aip(AIPEntity)
     * @param aipEntity
     */
    public void setAipEntity(AIPEntity aipEntity) {
        this.aipEntity = aipEntity;
    }

    /**
     * @return the associated aip
     */
    public AIP getAip() {
        return aipEntity.getAip();
    }

    /**
     * Set the associated aip
     * @param aip
     */
    public void setAip(AIP aip) {
        this.aipEntity = new AIPEntity(aip);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isQuicklook() {
        return dataType == DataType.QUICKLOOK_HD || dataType == DataType.QUICKLOOK_MD
                || dataType == DataType.QUICKLOOK_SD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        StorageDataFile dataFile = (StorageDataFile) o;

        if (checksum != null ? !checksum.equals(dataFile.checksum) : dataFile.checksum != null) {
            return false;
        }
        if (algorithm != null ? !algorithm.equals(dataFile.algorithm) : dataFile.algorithm != null) {
            return false;
        }
        return aipEntity != null ? aipEntity.equals(dataFile.aipEntity) : dataFile.aipEntity == null;
    }

    @Override
    public int hashCode() {
        int result = checksum != null ? checksum.hashCode() : 0;
        result = (31 * result) + (algorithm != null ? algorithm.hashCode() : 0);
        result = (31 * result) + (aipEntity != null ? aipEntity.hashCode() : 0);
        return result;
    }
}
