/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.domain;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;

/**
 *
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_acquisition_product",
        indexes = { @Index(name = "idx_acq_product_name", columnList = "product_name"),
                @Index(name = "idx_acq_ingest_chain", columnList = "ingest_chain"),
                @Index(name = "idx_acq_product_session", columnList = "session") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_acq_product_name", columnNames = "product_name") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@NamedEntityGraphs({
        @NamedEntityGraph(name = "graph.acquisition.file.complete",
                attributeNodes = @NamedAttributeNode(value = "fileList")),
        @NamedEntityGraph(name = "graph.metaproduct.complete",
                attributeNodes = {
                        @NamedAttributeNode(value = "metaProduct", subgraph = "graph.metaproduct.complete.metafiles"),
                        @NamedAttributeNode(value = "fileList") },
                subgraphs = { @NamedSubgraph(name = "graph.metaproduct.complete.metafiles",
                        attributeNodes = { @NamedAttributeNode(value = "metaFiles") }) })

})
public class Product implements IIdentifiable<Long> {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ProductSequence", initialValue = 1, sequenceName = "seq_product")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ProductSequence")
    private Long id;

    /**
     * The {@link Product} status
     */
    @Column(name = "product_state", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductState state;

    @Column(name = "sip_state", length = 32, nullable = false)
    @Convert(converter = SipStateConverter.class)
    private ISipState sipState;

    @Column(name = "last_update", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    /**
     * The product name
     */
    @NotBlank
    @Column(name = "product_name", length = 128)
    private String productName;

    /**
     * The session identifier that create the current product
     */
    @Column(name = "session", length = 128)
    private String session;

    /**
     * The {@link MetaProduct}
     */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_product_id"), updatable = false)
    private MetaProduct metaProduct;

    /**
     * {@link List} of file include in the {@link Product}
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_id"))
    private final Set<AcquisitionFile> fileList = new HashSet<AcquisitionFile>();

    @Column(columnDefinition = "jsonb", name = "json_sip")
    @Type(type = "jsonb")
    private SIP sip;

    @Column(name = "ingest_chain")
    private String ingestChain;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((productName == null) ? 0 : productName.hashCode()); // NOSONAR
        return result;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Product other = (Product) obj;
        if (productName == null) {
            if (other.productName != null) {
                return false;
            }
        } else if (!productName.equals(other.productName)) {
            return false;
        }
        return true;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    public void addAcquisitionFile(AcquisitionFile acqFile) {
        this.fileList.add(acqFile);
    }

    public Set<AcquisitionFile> getAcquisitionFile() {
        return this.fileList;
    }

    public void removeAcquisitionFile(AcquisitionFile acqFile) {
        this.fileList.remove(acqFile);
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SIP getSip() {
        return sip;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestProcessingChain) {
        this.ingestChain = ingestProcessingChain;
    }

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(productName);
        strBuilder.append(" - ");
        strBuilder.append(state);
        strBuilder.append(" - ");
        strBuilder.append(sipState);
        strBuilder.append(" - ");
        strBuilder.append(session);
        strBuilder.append(" - ");
        strBuilder.append(ingestChain);
        return strBuilder.toString();
    }

    public ISipState getSipState() {
        return sipState;
    }

    public void setSipState(ISipState sipState) {
        this.sipState = sipState;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}