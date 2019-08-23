package fr.cnes.regards.modules.ingest.domain.aip;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

@Entity
@Table(name = "t_aip",
        indexes = { @Index(name = "idx_aip_id", columnList = "id,aipId,sip_id"),
                @Index(name = "idx_aip_provider_id", columnList = "provider_id"),
                @Index(name = "idx_aip_session_owner", columnList = "session_owner"),
                @Index(name = "idx_aip_session", columnList = "session_name"),
                @Index(name = "idx_aip_ingest_chain", columnList = "ingest_chain"),
                @Index(name = "idx_aip_state", columnList = "state"),
                @Index(name = "idx_aip_last_update", columnList = "last_update"), })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPEntity {

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * The provider identifier is provided by the user along the SIP, with no guaranty of uniqueness,
     * and propagated to children (i.e. AIPs)
     */
    @NotBlank(message = "Provider ID is required")
    @Column(name = "provider_id", length = 100, nullable = false)
    private String providerId;

    /**
     * The AIP Internal identifier (generated URN)
     * versions
     */
    @NotBlank(message = "AIP URN is required")
    @Column(name = "aipId", length = SIPEntity.MAX_URN_SIZE)
    private String aipId;

    /**
     * Look at {@link IngestMetadata}
     */
    @Embedded
    private IngestMetadata ingestMetadata;

    /**
     * The SIP identifier which generate the current AIP
     */
    @NotNull(message = "Related SIP entity is required")
    @ManyToOne
    @JoinColumn(name = "sip_id", foreignKey = @ForeignKey(name = "fk_sip"))
    private SIPEntity sip;

    @NotNull(message = "AIP state is required")
    @Enumerated(EnumType.STRING)
    private AIPState state;

    @NotNull(message = "Creation date is required")
    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    @NotNull(message = "Last update date is required")
    @Column(name = "last_update", nullable = false)
    private OffsetDateTime lastUpdate;

    @Column(name = "error_message", length = 256)
    private String errorMessage;

    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> tags;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> categories;

    @NotNull(message = "RAW JSON AIP is required")
    @Column(columnDefinition = "jsonb", name = "rawaip", nullable = false)
    @Type(type = "jsonb")
    private AIP aip;

    public Long getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public SIPEntity getSip() {
        return sip;
    }

    public void setSip(SIPEntity sip) {
        this.sip = sip;
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public AIP getAip() {
        return aip;
    }

    public void setAip(AIP aip) {
        this.aip = aip;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public UniformResourceName getAipIdUrn() {
        return UniformResourceName.fromString(aipId);
    }

    public void setAipId(UniformResourceName aipId) {
        this.aipId = aipId.toString();
    }

    public IngestMetadata getIngestMetadata() {
        return ingestMetadata;
    }

    public void setIngestMetadata(IngestMetadata ingestMetadata) {
        this.ingestMetadata = ingestMetadata;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public static AIPEntity build(SIPEntity sip, AIPState state, AIP aip) {
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setAip(aip);
        aipEntity.setState(state);
        aipEntity.setSip(sip);
        aipEntity.setAipId(aip.getId());
        aipEntity.setCreationDate(OffsetDateTime.now());
        aipEntity.setLastUpdate(aipEntity.getCreationDate());
        // Extracted from SIP for search purpose
        aipEntity.setProviderId(sip.getProviderId());
        aipEntity.setIngestMetadata(sip.getIngestMetadata());
        // Extracted from AIP for search purpose
        aipEntity.setTags(new HashSet<>(aip.getTags()));
        aipEntity.setCategories(new HashSet<>(aip.getCategories()));
        return aipEntity;
    }
}
