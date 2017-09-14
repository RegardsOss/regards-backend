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
package fr.cnes.regards.modules.acquisition.domain.metadata;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.acquisition.domain.Product;

/**
 * This class reprensents a product type
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_meta_product", indexes = { @Index(name = "idx_chain_label", columnList = "label") },
        uniqueConstraints = @UniqueConstraint(name = "uk_chain_label", columnNames = { "label" }))
public class MetaProduct implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 64
     */
    private static final int MAX_STRING_LENGTH = 64;

    @Id
    @SequenceGenerator(name = "MetaProductSequence", initialValue = 1, sequenceName = "seq_meta_product")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MetaProductSequence")
    protected Long id;

    /**
     * Label to identify the {@link MetaProduct}
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_LENGTH, nullable = false)
    private String label;
    
    @NotNull
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_id"))
    private Set<Product> products = new HashSet<Product>();

    //    /**
    //     * La liste des type de fichiers composant ce produit (liste de MetaFile)
    //     */
    //    private List<MetaFile> metaFileList;

    //    /**
    //     * Les informations d'acquisition pour ce type de produit
    //     */
    //    private MetaProductAcquisitionInfos acquisitionInformations;

    //        /**
    //         * La fourniture a laquelle est rattache ce {@link MetaProduct}
    //         */
    //        private ChainGeneration supply_;

    /**
     * Default constructor
     */
    public MetaProduct() {
        super();
    }

    //    public MetaProductAcquisitionInfos getAcquisitionInformations() {
    //        return acquisitionInformations;
    //    }
    //
    //    public List<MetaFile> getMetaFileList() {
    //        return metaFileList;
    //    }

    //    public String getProductTypeName() {
    //        return productTypeName;
    //    }

    //    public void setAcquisitionInformations(MetaProductAcquisitionInfos pAcquisitionInformations) {
    //        acquisitionInformations = pAcquisitionInformations;
    //        // FIXME etudier autre possibilite d'initialisation
    //        if (acquisitionInformations.getMetaDataCreationPlugin() != null) {
    //            acquisitionInformations.getMetaDataCreationPlugin().setMProduct(this);
    //        }
    //    }

    //    public void setMetaFileList(List<MetaFile> pMetaFileList) {
    //        metaFileList = pMetaFileList;
    //        // for digester
    //        updateMetafileMetaproduct();
    //    }

    //    public ChainGeneration getSupply() {
    //        return supply_;
    //    }
    //
    //    public void setSupply(ChainGeneration pSupply) {
    //        supply_ = pSupply;
    //    }

    //    /**
    //     * Utilise lors de la creation par le digester. Vu les references cyclique le digester ne peut pas initialise le
    //     * metaproduct des metafiles.
    //     * 
    //     * @since 1.0
    //     */
    //    private void updateMetafileMetaproduct() {
    //        for (MetaFile metafile : metaFileList) {
    //            metafile.setMetaProduct(this);
    //
    //        }
    //    }

    @Override
    public Long getId() {
        return id;
    }
}
