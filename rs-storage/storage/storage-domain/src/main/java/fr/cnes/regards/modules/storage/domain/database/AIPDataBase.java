package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.Event;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_aip", indexes = {
        //        @Index(name = "idx_aip_ip_id", columnList = "ip_id")
})
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@NamedEntityGraph(name = "graph.aip.tags", attributeNodes = { @NamedAttributeNode("tags") })
public class AIPDataBase {

    /**
     * length used as the checksum column definition. Why 128? it allows to use sha-512. That should limit issues with checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    private static final int MAX_URN_SIZE = 128;

    //    @Id
    //    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    //    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @Id
    @Column(name = "ip_id", length = MAX_URN_SIZE, unique = true)
    private String ipId;

    @Column(name = "sip_id", length = MAX_URN_SIZE)
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

    public AIPDataBase() {
    }

    public AIPDataBase(AIP aip) {
        this.ipId = aip.getIpId();
        this.sipId = aip.getSipId();
        this.tags = Sets.newHashSet(aip.getTags());
        this.state = aip.getState();
        this.lastEvent = aip.getLastEvent();
        this.submissionDate = aip.getSubmissionEvent().getDate();
        this.aip = aip;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AIPDataBase that = (AIPDataBase) o;

        return ipId != null ? ipId.equals(that.ipId) : that.ipId == null;
    }

    @Override
    public int hashCode() {
        return ipId != null ? ipId.hashCode() : 0;
    }
}
