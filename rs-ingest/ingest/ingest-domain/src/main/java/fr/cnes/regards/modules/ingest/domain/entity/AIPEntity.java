package fr.cnes.regards.modules.ingest.domain.entity;

import java.time.OffsetDateTime;

import javax.persistence.Column;
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

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIP;

@Entity
@Table(name = "t_aip", indexes = { @Index(name = "idx_aip_id", columnList = "id,aipId,sip_id"),
        @Index(name = "idx_aip_state", columnList = "state") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPEntity {

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * The AIP Internal identifier (generated URN)
     * versions
     */
    @NotBlank(message = "AIP URN is required")
    @Column(name = "aipId", length = SIPEntity.MAX_URN_SIZE)
    private String aipId;

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
    @Column(name = "creation_date")
    private OffsetDateTime creationDate;

    @Column(name = "error_message", length = 256)
    private String errorMessage;

    @NotNull(message = "RAW JSON AIP is required")
    @Column(columnDefinition = "jsonb", name = "rawaip")
    @Type(type = "jsonb")
    private AIP aip;

    public Long getId() {
        return id;
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

    public static AIPEntity build(SIPEntity sip, AIPState state, AIP aip) {
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setAip(aip);
        aipEntity.setState(state);
        aipEntity.setSip(sip);
        aipEntity.setAipId(aip.getId());
        aipEntity.setCreationDate(OffsetDateTime.now());
        return aipEntity;
    }
}
