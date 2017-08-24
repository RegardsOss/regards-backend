package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.*;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_aip")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPDataBase {

    /**
     * length used as the checksum column definition. Why 128? it allows to use sha-512. That should limit issues with checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    private static final int MAX_URN_SIZE = 128;

    //    @Id
    //    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    //    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    //    private Long id;

    @Column(length = CHECKSUM_MAX_LENGTH)
    private String checksum;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @Id
    @Column(length = MAX_URN_SIZE)
    private String ipId;

    @Column(length = MAX_URN_SIZE)
    private String sipId;

    @ElementCollection
    private Set<String> tags;

    /**
     * State of this AIP
     */
    @Column
    @Enumerated(EnumType.STRING)
    private AIPState state;

    /**
     * Last Event that affected this AIP
     */
    @Embedded
    private Event lastEvent;

    /**
     * Submission Date into REGARDS
     */
    @Column
    private OffsetDateTime submissionDate;

    @Column(columnDefinition = "jsonb", name = "json_aip")
    @Type(type = "jsonb")
    private AIP aip;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_storage_plugin_configuration",
            foreignKey = @ForeignKey(name = "fk_aip_data_storage_plugin_configuration"))
    private PluginConfiguration dataStorageConf;

    /**
     * It seems that CacadeType.PERSIST is not enough here, so we use MERGE too. Without MERGE, hibernate/spring tries to save DataFile with null values
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "aip_ip_id", foreignKey = @ForeignKey(name = "fk_aip_data_file"))
    private Set<DataFile> dataFiles;

    public AIPDataBase() {
    }

    public AIPDataBase(AIP aip, PluginConfiguration dataStorageConf) {
        this.checksum = aip.getChecksum();
        this.ipId = aip.getIpId();
        this.sipId = aip.getSipId();
        this.tags = Sets.newHashSet(aip.getTags());
        this.state = aip.getState();
        this.lastEvent = aip.getLastEvent();
        this.submissionDate = aip.getSubmissionEvent().getDate();
        this.aip = aip;
        if (dataStorageConf != null) {
            this.dataStorageConf = dataStorageConf;
        }
        Set<DataFile> dataFiles = Sets.newHashSet();
        for (InformationObject io : aip.getInformationObjects()) {
            DataObject file = io.getContentInformation().getDataObject();
            MimeType mimeType = MimeType.valueOf(io.getContentInformation().getRepresentationInformation().getSyntax().getMimeType());
            String algorithm = io.getPdi().getFixityInformation().getAlgorithm();
            String checksum = io.getPdi().getFixityInformation().getChecksum();
            Double fileSize = io.getPdi().getFixityInformation().getFileSize();
            dataFiles.add(new DataFile(file, algorithm, checksum, fileSize, mimeType));
        }
        this.dataFiles = dataFiles;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public Event getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(Event lastEvent) {
        this.lastEvent = lastEvent;
    }

    public OffsetDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(OffsetDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public AIP getAip() {
        return aip;
    }

    public void setAip(AIP aip) {
        this.aip = aip;
    }
}
