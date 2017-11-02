package fr.cnes.regards.modules.ingest.domain.entity;

import java.time.OffsetDateTime;

import javax.persistence.CascadeType;
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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.storage.domain.AIP;

@Entity
@Table(name = "t_aip", indexes = { @Index(name = "idx_aip_id", columnList = "id,sip_id") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPEntity {

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * The SIP identifier which generate the current AIP
     */
    @NotNull
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "sip_id", foreignKey = @ForeignKey(name = "fk_sip"))
    private SIPEntity sip;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AIPState state;

    @NotNull
    @Column(name = "creation_date")
    private OffsetDateTime creationDate;

    @NotNull
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

}
