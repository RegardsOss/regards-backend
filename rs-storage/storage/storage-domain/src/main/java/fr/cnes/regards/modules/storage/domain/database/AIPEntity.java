package fr.cnes.regards.modules.storage.domain.database;

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Metadata of an AIP metadata.
 * It was not necessary to map all the AIP structure into the database so we just mapped some metadata and added the
 * whole AIP as a json field.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_aip",
        indexes = { @Index(name = "idx_aip_ip_id", columnList = "ip_id"),
                @Index(name = "idx_aip_state", columnList = "state"),
                @Index(name = "idx_aip_submission_date", columnList = "submissionDate"),
                @Index(name = "idx_aip_last_event_date", columnList = "date") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_aip_ipId", columnNames = "ip_id") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@NamedEntityGraph(name = "graph.aip.tags", attributeNodes = { @NamedAttributeNode("tags") })
public class AIPEntity {

    /**
     * Max urn size
     */
    private static final int MAX_URN_SIZE = 128;

    /**
     * The id
     */
    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @Column(name = "ip_id", length = MAX_URN_SIZE)
    private String ipId;

    /**
     * The aip sip id
     */
    @Column(name = "sip_id", length = MAX_URN_SIZE)
    private String sipId;

    /**
     * The aip metadata tags
     */
    @ElementCollection
    @CollectionTable(name = "t_aip_tag", joinColumns = @JoinColumn(name = "aip_id"),
            foreignKey = @javax.persistence.ForeignKey(name = "fk_aip_tag_aip_id"))
    private Set<String> tags;

    /**
     * State of this AIP
     */
    @Column
    @Enumerated(EnumType.STRING)
    private AIPState state;

    /**
     * Whether to retry storage after a storage error
     */
    private boolean retry;

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

    /**
     * The actual aip metadata
     */
    @Column(columnDefinition = "jsonb", name = "json_aip")
    @Type(type = "jsonb")
    private AIP aip;

    /**
     * Default constructor
     */
    public AIPEntity() {
    }

    /**
     * Constructor initializing the aip entity from an actual aip
     * @param aip
     */
    public AIPEntity(AIP aip) {
        this.ipId = aip.getId().toString();
        this.sipId = aip.getSipId();
        this.tags = Sets.newHashSet(aip.getTags());
        this.state = aip.getState();
        this.lastEvent = aip.getLastEvent();
        this.submissionDate = aip.getSubmissionEvent().getDate();
        this.aip = aip;
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
     * @return the ip id
     */
    public String getIpId() {
        return ipId;
    }

    /**
     * Set the ip id
     * @param ipId
     */
    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    /**
     * @return the sip id
     */
    public String getSipId() {
        return sipId;
    }

    /**
     * set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    /**
     * @return the tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Set the tags
     * @param tags
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * @return the state
     */
    public AIPState getState() {
        return state;
    }

    /**
     * Set the state
     * @param state
     */
    public void setState(AIPState state) {
        this.state = state;
    }

    /**
     * @return the last event
     */
    public Event getLastEvent() {
        return lastEvent;
    }

    /**
     * Set the last event
     * @param lastEvent
     */
    public void setLastEvent(Event lastEvent) {
        this.lastEvent = lastEvent;
    }

    /**
     * @return the submission date
     */
    public OffsetDateTime getSubmissionDate() {
        return submissionDate;
    }

    /**
     * Set the submission date
     * @param submissionDate
     */
    public void setSubmissionDate(OffsetDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    /**
     * @return the aip
     */
    public AIP getAip() {
        return aip;
    }

    /**
     * Set the aip
     * @param aip
     */
    public void setAip(AIP aip) {
        this.aip = aip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        AIPEntity that = (AIPEntity) o;

        return ipId != null ? ipId.equals(that.ipId) : that.ipId == null;
    }

    @Override
    public int hashCode() {
        return ipId != null ? ipId.hashCode() : 0;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
