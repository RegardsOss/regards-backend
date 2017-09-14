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

    @Column
    private Boolean active = false;

    @Column(name = "last_date_activation")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastDateActivation;

    /**
     * periodicity in seconds
     */
    @Column(name = "period")
    private Long periodicity;

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "metaproduct_id", foreignKey = @ForeignKey(name = "fk_metaproduct_id"), nullable = true,
            updatable = true)
    private MetaProduct metaProduct;

    @Column(length = MAX_STRING_LENGTH, nullable = false)
    private String dataSet;

    @Column(name = "comment")
    @Type(type = "text")
    private String comment;

    /**
     * MD5 Signature of file product have to be calculate or not
     */
    @Column
    private Boolean calculteMd5Signature;

    public ChainGeneration() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

}
