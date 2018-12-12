package fr.cnes.regards.modules.storage.domain.database;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Metadata of an AIP metadata.
 * It was not necessary to map all the AIP structure into the database so we just mapped some metadata and added the
 * whole AIP as a json field.
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_aip",
        indexes = { @Index(name = "idx_aip_ip_id", columnList = "aip_id"),
                @Index(name = "idx_aip_state", columnList = "state"),
                @Index(name = "idx_aip_submission_date", columnList = "submission_date"),
                @Index(name = "idx_aip_last_event_date", columnList = "date") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_aip_ipId", columnNames = "aip_id") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@NamedEntityGraphs({ @NamedEntityGraph(name = "graph.aip.with.session",
        attributeNodes = { @NamedAttributeNode(value = "session") }) })
public class AIPEntity {

    /**
     * Max urn size
     */
    public static final int MAX_URN_SIZE = 128;

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
    @Column(name = "aip_id", length = MAX_URN_SIZE)
    private String aipId;

    /**
     * The aip sip id
     */
    @Column(name = "sip_id", length = MAX_URN_SIZE)
    private String sipId;

    /**
     * Provider id
     */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    /**
     * AIP state
     */
    @Column(length = 32)
    @Enumerated(EnumType.STRING)
    private AIPState state;

    /**
     * Whether to retry storage after a storage error
     */
    @Column
    private boolean retry;

    /**
     * Last Event that affected this AIP
     */
    @Embedded
    private Event lastEvent;

    /**
     * Submission Date into REGARDS
     */
    @Column(name = "submission_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime submissionDate;

    /**
     * The actual aip metadata
     */
    @Column(columnDefinition = "jsonb", name = "json_aip")
    @Type(type = "jsonb")
    private AIP aip;

    /**
     * The REGARDS session identifier used while creating this entity
     */
    @NotNull(message = "Session is required")
    @ManyToOne
    @JoinColumn(name = "session", foreignKey = @ForeignKey(name = "fk_aip_session"))
    private AIPSession session;

    /**
     * Default constructor
     */
    public AIPEntity() {
    }

    /**
     * Constructor initializing the aip entity from an actual aip
     * @param aip
     * @param aipSession
     */
    public AIPEntity(AIP aip, AIPSession aipSession) {
        this.aipId = aip.getId().toString();
        this.sipId = aip.getSipId().orElse(null);
        this.providerId = aip.getProviderId();
        this.state = aip.getState();
        this.lastEvent = aip.getLastEvent();
        this.submissionDate = aip.getSubmissionEvent().getDate();
        this.aip = aip;
        this.session = aipSession;
        this.retry = aip.isRetry();
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
    public String getAipId() {
        return aipId;
    }

    public UniformResourceName getAipIdUrn() {
        return UniformResourceName.fromString(aipId);
    }

    /**
     * Set the ip id
     * @param aipId
     */
    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public void setAipId(UniformResourceName aipId) {
        this.aipId = aipId.toString();
    }

    /**
     * @return the sip id
     */
    public String getSipId() {
        return sipId;
    }

    public UniformResourceName getSipIdUrn() {
        return UniformResourceName.fromString(sipId);
    }

    /**
     * set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public void setSipId(UniformResourceName sipId) {
        this.sipId = sipId.toString();
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
     * @return the session identifier
     */
    public AIPSession getSession() {
        return session;
    }

    /**
     * set the session identifier
     * @param session session id
     */
    public void setSession(AIPSession session) {
        this.session = session;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AIPEntity that = (AIPEntity) o;

        return aipId != null ? aipId.equals(that.aipId) : that.aipId == null;
    }

    @Override
    public int hashCode() {
        return aipId != null ? aipId.hashCode() : 0;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
