package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.Event;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_aip")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPDataBase {

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    @Column(length = 32)
    private String checksum;

    @Column
    private String ipId;

    @Column
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
    @JoinColumn(name = "data_storage_plugin_configuration", foreignKey = @ForeignKey(name = "fk_aip_data_storage_plugin_configuration"))
    private PluginConfiguration dataStorageConf;

    //    private Set<DataObject> dataFiles;

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
        if(dataStorageConf!=null) {
            this.dataStorageConf=dataStorageConf;
        }
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
