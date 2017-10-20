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

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
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
@Table(name = "t_meta_product", indexes = { @Index(name = "idx_meta_product_label", columnList = "label") },
        uniqueConstraints = @UniqueConstraint(name = "uk_meta_product_label", columnNames = { "label" }))
@NamedEntityGraph(name = "graph.product.complete", attributeNodes = @NamedAttributeNode(value = "products"))
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

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_meta_product_id"))
    private Set<Product> products = new HashSet<Product>();

    /**
     * Algorithm used to calculate the checksum
     * see {@link MessageDigest}
     */
    @Column(name = "checksumAlgorithm", length = 16)
    private String checksumAlgorithm;

    /**
     * <code>true</code> clean the original file
     * TODO CMZ : util ici ? non je ne pense pas
     */
    @NotNull
    @Column(name = "cleanOriginalFile")
    private Boolean cleanOriginalFile = Boolean.TRUE;

    /**
     * The {@link List} of {@link MetaFile} for this {@link MetaProduct}
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_product_id", foreignKey = @ForeignKey(name = "fk_meta_product_id"))
    private Set<MetaFile> metaFiles = new HashSet<MetaFile>();

    //    /**
    //     * The last activation date when an acquisition were running 
    //     */
    //    @Transient
    //    private OffsetDateTime lastAcqDate;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MetaProduct other = (MetaProduct) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        return true;
    }

    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getCleanOriginalFile() {
        return cleanOriginalFile;
    }

    public void setCleanOriginalFile(Boolean clean) {
        this.cleanOriginalFile = clean;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        this.products.add(product);
    }

    public void removeProduct(Product product) {
        this.products.remove(product);
    }

    public Set<MetaFile> getMetaFiles() {
        return metaFiles;
    }

    public void addMetaFile(MetaFile metaFile) {
        this.metaFiles.add(metaFile);
    }

    public void removeMetaFile(MetaFile metaFile) {
        this.metaFiles.remove(metaFile);
    }

    public void setId(Long id) {
        this.id = id;
    }

    //    public OffsetDateTime getLastAcqDate() {
    //        return lastAcqDate;
    //    }
    //
    //    public void setLastAcqDate(OffsetDateTime lastAcqDate) {
    //        this.lastAcqDate = lastAcqDate;
    //    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(label);
        return strBuilder.toString();
    }

}
