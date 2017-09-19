package fr.cnes.regards.modules.storage.domain.database;

import java.net.URL;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.util.MimeType;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 *
 * contains useful, for the system, metadata of a file from an AIP.
 * It mixes {@link DataObject}, {@link FixityInformation}, {@link RepresentationInformation} and add some information needed by the system not extracted from the AIP
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_data_file")
@NamedEntityGraph(name = "graph.datafile.full",
        attributeNodes = { @NamedAttributeNode("aipDataBase"),
                @NamedAttributeNode(value = "dataStorageUsed", subgraph = "graph.datafile.dataStorageUsed") },
        subgraphs = {
                @NamedSubgraph(name = "graph.datafile.dataStorageUsed",
                        attributeNodes = { @NamedAttributeNode(value = "parameters",
                                subgraph = "graph.datafile.dataStorageUsed.parameters") }),
                @NamedSubgraph(name = "graph.datafile.dataStorageUsed.parameters",
                        attributeNodes = { @NamedAttributeNode("dynamicsValues") }) })
public class DataFile {

    @Id
    @SequenceGenerator(name = "dataFileSequence", initialValue = 1, sequenceName = "seq_data_file")
    @GeneratedValue(generator = "dataFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private URL url;

    @Column
    private String name;

    @Column(length = AIPDataBase.CHECKSUM_MAX_LENGTH, nullable = false)
    private String checksum;

    @Column(nullable = false)
    private String algorithm;

    @Column
    @Enumerated(EnumType.STRING)
    private DataType dataType;

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

    // serialization
    private DataFile() {
    }

    public DataFile(DataObject file, String algorithm, String checksum, Long fileSize, MimeType mimeType, AIP aip) {
        this(file.getUrl(), checksum, algorithm, file.getDataType(), fileSize, mimeType, aip, null);
        String name = file.getFilename();
        if (Strings.isNullOrEmpty(name)) {
            String[] pathParts = file.getUrl().getPath().split("/");
            name = pathParts[pathParts.length - 1];
        }
        setName(name);
    }

    public DataFile(URL url, String checksum, String algorithm, DataType type, Long fileSize, MimeType mimeType,
            AIP aip, String name) {
        this.url = url;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.dataType = type;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.aipDataBase = new AIPDataBase(aip);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
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

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType type) {
        this.dataType = type;
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

    public AIPDataBase getAipDataBase() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        DataFile dataFile = (DataFile) o;

        if (checksum != null ? !checksum.equals(dataFile.checksum) : dataFile.checksum != null) {
            return false;
        if (algorithm != null ? !algorithm.equals(dataFile.algorithm) : dataFile.algorithm != null)
            return false;
        return aipDataBase != null ? aipDataBase.equals(dataFile.aipDataBase) : dataFile.aipDataBase == null;
    }

    @Override
    public int hashCode() {
        int result = checksum != null ? checksum.hashCode() : 0;
        result = 31 * result + (algorithm != null ? algorithm.hashCode() : 0);
        result = 31 * result + (aipDataBase != null ? aipDataBase.hashCode() : 0);
        return result;
    }
}
