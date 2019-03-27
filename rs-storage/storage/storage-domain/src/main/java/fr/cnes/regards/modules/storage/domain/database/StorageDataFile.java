package fr.cnes.regards.modules.storage.domain.database;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Min;

import org.springframework.util.MimeType;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.jpa.converter.SetStringCsvConverter;
import fr.cnes.regards.framework.jpa.converter.SetURLCsvConverter;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
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
@Table(name = "t_data_file",
        indexes = { @Index(name = "idx_data_file_checksum", columnList = "checksum"),
                @Index(name = "idx_data_file_state", columnList = "state"),
                @Index(name = "idx_data_file_aip", columnList = "aip_ip_id") })
// @formatter:off
@NamedEntityGraph(name = "graph.datafile.full", attributeNodes = {
            @NamedAttributeNode("aipEntity"),
            @NamedAttributeNode(value = "prioritizedDataStorages", subgraph = "graph.datafile.prioritizedDataStorages")
        },
        subgraphs = {
                @NamedSubgraph(name = "graph.datafile.prioritizedDataStorages",
                        attributeNodes = { @NamedAttributeNode(value = "dataStorageConfiguration") }
                )
        })
// @formatter:on
public class StorageDataFile {

    /**
     * length used as the checksum column definition. Why 128? it allows to use sha-512. That should limit issues with
     * checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    public static final String NOT_VALID_CHECKSUM_SUFFIX = "_nvc";

    @Convert(converter = SetStringCsvConverter.class)
    @Column(name = "failure_causes")
    private final Set<String> failureCauses = new HashSet<>();

    /**
     * The id
     */
    @Id
    @SequenceGenerator(name = "dataFileSequence", initialValue = 1, sequenceName = "seq_data_file")
    @GeneratedValue(generator = "dataFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * File urls
     */
    @Column(columnDefinition = "text")
    @Convert(converter = SetURLCsvConverter.class)
    private Set<URL> urls = new HashSet<>();

    /**
     * File origin urls
     */
    @Column(columnDefinition = "text", name = "origin_urls")
    @Convert(converter = SetURLCsvConverter.class)
    private Set<URL> originUrls = new HashSet<>();

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
    @Column(name = "data_type")
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    /**
     * File size
     */
    @Column(name = "file_size")
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
    @Column(nullable = false, name = "mime_type")
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    @Column
    private Integer height;

    @Column
    private Integer width;

    /**
     * Directory to use for storage. Can be null.
     * This parameter should be set by the IAllocationStrategy plugin during storage dispatch.
     */
    @Column(name = "storage_directory")
    private String storageDirectory;

    /**
     * Data storage plugin configuration used to store the file
     */
    @ManyToMany
    @JoinTable(name = "ta_data_file_plugin_conf",
            joinColumns = @JoinColumn(name = "data_file_id",
                    foreignKey = @ForeignKey(name = "fk_data_file_plugin_conf_data_file")),
            inverseJoinColumns = @JoinColumn(name = "data_storage_conf_id",
                    foreignKey = @ForeignKey(name = "fk_plugin_conf_data_file_plugin_conf")))
    private final Set<PrioritizedDataStorage> prioritizedDataStorages = new HashSet<>();

    /**
     * Reversed mapping compared to reality. This is because it is easier to work like this.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aip_ip_id", foreignKey = @ForeignKey(name = "fk_aip_data_file"))
    private AIPEntity aipEntity;

    /**
     * Indicates the number of archives that have to store this data file. Archives <=> IDataStorage configurations
     */
    @Column(name = "not_yet_stored_by")
    @Min(value = 0, message = "Attribute notYetStoredBy cannot be negative. Actual value : ${validatedValue}")
    private Long notYetStoredBy = 0L;

    /**
     * Indicates the number of archives that have to delete this data file. Archives <=> IDataStorage configurations.
     * This attribute is only used with state {@link DataFileState#PARTIAL_DELETION_PENDING}. In case of total deletion,
     * this value is irrelevant.
     */
    @Column(name = "not_yet_deleted_by")
    @Min(value = 0, message = "Attribute notYetDeletedBy cannot be negative. Actual value : ${validatedValue}")
    private Long notYetDeletedBy = 0L;

    /**
     * Default constructor
     */
    @SuppressWarnings("unused")
    private StorageDataFile() {
        // serialization
    }

    /**
     * Initialize the data file from the parameters
     * @param file
     * @param mimeType
     * @param aipEntity
     */
    public StorageDataFile(OAISDataObject file, MimeType mimeType, AIPEntity aipEntity) {
        this(file.getUrls(), file.getChecksum(), file.getAlgorithm(), file.getRegardsDataType(), file.getFileSize(),
             mimeType, aipEntity, null, null);
        String name = file.getFilename();
        if (Strings.isNullOrEmpty(name)) {
            String[] pathParts = file.getUrls().iterator().next().getPath().split("/");
            name = pathParts[pathParts.length - 1];
        }
        this.name = name;
    }

    /**
     * Constructor setting the parameters as attribute except for the aip which is transformed to a {@link AIPEntity}
     * @param urls
     * @param checksum
     * @param algorithm
     * @param type
     * @param fileSize
     * @param mimeType
     * @param aipEntity
     * @param name
     */
    public StorageDataFile(Set<URL> urls, String checksum, String algorithm, DataType type, Long fileSize,
            MimeType mimeType, AIPEntity aipEntity, String name, String storageDirectory) {
        if (urls != null) {
            this.urls.addAll(urls);
        }
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.dataType = type;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.aipEntity = aipEntity;
        this.name = name;
        this.storageDirectory = storageDirectory;
    }

    /**
     * Extract the different file metadata from an aip
     * @param aip
     * @return extracted data files
     */
    public static Set<StorageDataFile> extractDataFiles(AIP aip, AIPSession aipSession) {
        return extractDataFilesForExistingAIP(aip, new AIPEntity(aip, aipSession));
    }

    public static Set<StorageDataFile> extractDataFilesForExistingAIP(AIP aip, AIPEntity aipEntity) {
        Set<StorageDataFile> dataFiles = Sets.newHashSet();
        for (ContentInformation ci : aip.getProperties().getContentInformations()) {
            OAISDataObject file = ci.getDataObject();
            if ((file != null) && !file.isReference()) {
                // Only non reference data object is managed by storage
                MimeType mimeType = ci.getRepresentationInformation().getSyntax().getMimeType();
                dataFiles.add(new StorageDataFile(file, mimeType, aipEntity));
            }
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
     * @return the urls
     */
    public Set<URL> getUrls() {
        if (urls == null) {
            urls = new HashSet<>();
        }
        return urls;
    }

    /**
     * Set the urls
     * @param urls
     */
    public void setUrls(Set<URL> urls) {
        this.urls = urls;
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
    public Set<PrioritizedDataStorage> getPrioritizedDataStorages() {
        return prioritizedDataStorages;
    }

    /**
     * Set the data storage plugin configuration
     * @param dataStorageUsed
     */
    public void addDataStorageUsed(PrioritizedDataStorage dataStorageUsed) {
        this.prioritizedDataStorages.add(dataStorageUsed);
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
    public void setAip(AIP aip, AIPSession aipSession) {
        this.aipEntity = new AIPEntity(aip, aipSession);
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Does the file need to be store in an online archive ?
     * @return [true|false]
     */
    public boolean isOnlineMandatory() {
        switch (dataType) {
            case THUMBNAIL:
            case DOCUMENT:
            case QUICKLOOK_HD:
            case QUICKLOOK_MD:
            case QUICKLOOK_SD:
                return true;
            default:
                return false;
        }
    }

    public boolean isQuicklook() {
        switch (dataType) {
            case QUICKLOOK_HD:
            case QUICKLOOK_MD:
            case QUICKLOOK_SD:
                return true;
            default:
                return false;
        }
    }

    public Long getNotYetStoredBy() {
        return notYetStoredBy;
    }

    public Long getNotYetDeletedBy() {
        return notYetDeletedBy;
    }

    public String getStorageDirectory() {
        return storageDirectory;
    }

    public void setStorageDirectory(String directory) {
        this.storageDirectory = directory;
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
        if (dataType != dataFile.dataType) {
            return false;
        }
        return aipEntity != null ? aipEntity.equals(dataFile.aipEntity) : dataFile.aipEntity == null;
    }

    @Override
    public int hashCode() {
        int result = checksum != null ? checksum.hashCode() : 0;
        result = (31 * result) + (algorithm != null ? algorithm.hashCode() : 0);
        result = (31 * result) + (dataType != null ? dataType.hashCode() : 0);
        result = (31 * result) + (aipEntity != null ? aipEntity.hashCode() : 0);
        return result;
    }

    public void increaseNotYetStoredBy() {
        notYetStoredBy++;
    }

    public void decreaseNotYetStoredBy() throws EntityOperationForbiddenException {
        if (notYetStoredBy > 0L) {
            notYetStoredBy--;
        } else {
            throw new EntityOperationForbiddenException(
                    String.format("Forbidden decrease <notYetStoredBy> for dataFile %s - %s", this.id, this.name));
        }
    }

    public void increaseNotYetDeletedBy() {
        notYetDeletedBy++;
    }

    public void decreaseNotYetDeletedBy() throws EntityOperationForbiddenException {
        if (notYetDeletedBy > 0L) {
            notYetDeletedBy--;
        } else {
            throw new EntityOperationForbiddenException(
                    String.format("Forbidden decrease <notYetDeletedBy> for dataFile %s - %s", this.id, this.name));
        }
    }

    public void emptyFailureCauses() {
        this.failureCauses.clear();
    }

    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    public void addFailureCause(String failureCause) {
        if (failureCause != null) {
            this.failureCauses.add(failureCause);
        }
    }

    public Set<URL> getOriginUrls() {
        return originUrls;
    }

    public void setOriginUrls(Set<URL> originUrls) {
        this.originUrls = originUrls;
    }

    public void resetNotYetStoredBy() {
        notYetStoredBy = 0L;
    }
}
