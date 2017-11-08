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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_product")
@NamedEntityGraph(name = "graph.acquisition.file.complete", attributeNodes = @NamedAttributeNode(value = "fileList"))
public class Product implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 64
     */
    private static final int MAX_STRING_LENGTH = 128;

    /**
     * Maximum enum size constraint with length 16
     */
    private static final int MAX_ENUM_LENGTH = 16;

    //    /**
    //     * TODO CMZ IDENT_PRODUCT_PREFIX à virer
    //     * Prefixe d'identification des fichiers descripteurs de fichier
    //     */
    //    public static final String IDENT_PRODUCT_PREFIX = "PRODUCT_";

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
    @Column(name = "status", length = MAX_ENUM_LENGTH)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    /**
     * <li><code>true</code> if the {@link Product} has been saved by ingest</br>
     * <li><code>false</code> otherwise
     */
    @Column(name = "send")
    private Boolean send = false;

    /**
     * The product name
     */
    @NotBlank
    @Column(name = "product_name", length = MAX_STRING_LENGTH)
    private String productName;

    /**
     * The session identifier that create the current product 
     */
    @Column(length = MAX_STRING_LENGTH)
    private String session;

    /**
     * The {@link MetaProduct}
     */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "meta_product_id", referencedColumnName = "ID", foreignKey = @ForeignKey(name = "fk_product_id"),
            updatable = false)
    private MetaProduct metaProduct;

    //    /**
    //     * numero de version du produit
    //     */
    //    private int version;

    //    /**
    //     * nom du fichier de meta donnee du produit
    //     */
    //    private DescriptorFile metaDataFileName_;

    /**
     * Liste des fichiers composants les produits
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "ID", foreignKey = @ForeignKey(name = "fk_product_id"))
    private Set<AcquisitionFile> fileList = new HashSet<AcquisitionFile>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((productName == null) ? 0 : productName.hashCode());
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

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
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

    public Boolean isSend() {
        return send;
    }

    public void setSend(Boolean send) {
        this.send = send;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(id);
        strBuilder.append(" - ");
        strBuilder.append(productName);
        strBuilder.append(" - ");
        strBuilder.append(status);
        return strBuilder.toString();
    }

}