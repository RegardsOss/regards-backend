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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_generation_chain", indexes = { @Index(name = "idx_chain_label", columnList = "label") },
        uniqueConstraints = @UniqueConstraint(name = "uk_chain_label", columnNames = { "label" }))
public class ChainGeneration implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 128
     */
    private static final int MAX_STRING_LENGTH = 128;

    /**
     * entity id for SGBD purpose mainly and REST request
     */
    @Id
    @SequenceGenerator(name = "ChainSequence", initialValue = 1, sequenceName = "seq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ChainSequence")
    protected Long id;

    /**
     * Label to identify the {@link ChainGeneration}
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_LENGTH, nullable = false)
    private String label;

    /**
     * <code>true</code> if the {@link ChainGeneration} is active, <code>false</code> otherwise
     */
    @Column
    private Boolean active = false;

    /**
     * The last activation date when an acquisition were running 
     */
    @Column(name = "last_date_activation")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastDateActivation;

    /**
     * The periodicity in seconds between to acquisition
     */
    @Column(name = "period")
    private Long periodicity;

    /**
     * THe {@link MetaProduct} used for this {@link ChainGeneration}
     */
    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_metaproduct_id"), nullable = true,
            updatable = true)
    private MetaProduct metaProduct;

    /**
     * The dataset for which the acquired files are set  
     */
    @Column(length = MAX_STRING_LENGTH)
    private String dataSet;

    /**
     * A comment
     */
    @Column(name = "comment")
    @Type(type = "text")
    private String comment;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((active == null) ? 0 : active.hashCode());
        result = prime * result + ((dataSet == null) ? 0 : dataSet.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChainGeneration other = (ChainGeneration) obj;
        if (active == null) {
            if (other.active != null)
                return false;
        } else if (!active.equals(other.active))
            return false;
        if (dataSet == null) {
            if (other.dataSet != null)
                return false;
        } else if (!dataSet.equals(other.dataSet))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getLastDateActivation() {
        return lastDateActivation;
    }

    public void setLastDateActivation(OffsetDateTime lastDateActivation) {
        this.lastDateActivation = lastDateActivation;
    }

    public Long getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Long periodicity) {
        this.periodicity = periodicity;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public void setMetaProduct(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(label);
        strBuilder.append(" - ");
        strBuilder.append(dataSet);
        strBuilder.append(" - [");
        strBuilder.append(metaProduct.toString());
        strBuilder.append("]");
        return strBuilder.toString();
    }

}
