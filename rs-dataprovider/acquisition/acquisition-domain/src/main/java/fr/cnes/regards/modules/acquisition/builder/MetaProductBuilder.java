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
package fr.cnes.regards.modules.acquisition.builder;

import java.util.Set;

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * {@link MetaFile} builder
 *
 * @author Christophe Mertz
 *
 */
public final class MetaProductBuilder {

    /**
     * Current {@link MetaProduct}
     */
    private final MetaProduct metaProduct;

    private MetaProductBuilder(MetaProduct metaProduct) {
        this.metaProduct = metaProduct;
    }

    /**
     * Create a {@link MetaProductBuilder}
     * @param label the label {@link String} value
     * @return the current {@link MetaProductBuilder}
     */
    public static MetaProductBuilder build(String label) {
        final MetaProduct mp = new MetaProduct();
        mp.setLabel(label);
        return new MetaProductBuilder(mp);
    }

    /**
     * Set the {@link Product} property to the current {@link MetaProduct}
     * @param product the {@link MetaProduct}
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder addProduct(Product product) {
        metaProduct.addProduct(product);
        return this;
    }

    /**
     * Set the {@link MetaFile} property to the current {@link MetaProduct}
     * @param metaFile the {@link MetaFile}
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder addMetaFile(MetaFile metaFile) {
        metaProduct.addMetaFile(metaFile);
        return this;
    }

    /**
     * Set the {@link Set} of {@link MetaFile} property to the current {@link MetaProduct}
     * @param metaFile a {@link Set} of {@link MetaFile}
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder withMetaFiles(Set<MetaFile> metaFiles) {
        metaProduct.setMetaFiles(metaFiles);
        return this;
    }

    /**
     * Set the checksumAlgorithm property to the current {@link MetaProduct}
     * @param algo the checksum algorithm used to calculated the checksum
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder withChecksumAlgorithm(String algo) {
        metaProduct.setChecksumAlgorithm(algo);
        return this;
    }

    /**
     * Set the cleanOriginalFile property to the current {@link MetaProduct}
     * @param clean the cleanOriginalFile value
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder withCleanOriginalFile(Boolean clean) {
        metaProduct.setCleanOriginalFile(clean);
        return this;
    }

    /**
     * Set the ingest processing chain property to the current {@link MetaProduct}
     * @param ingestChain the ingest processing chain {@link String} value
     * @return the current {@link MetaProductBuilder}
     */
    public MetaProductBuilder withIngestProcessingChain(String ingestChain) {
        metaProduct.setIngestChain(ingestChain);
        return this;
    }

    /**
     * Get the current {@link MetaProduct}
     * @return the current {@link MetaProduct}
     */
    public MetaProduct get() {
        return metaProduct;
    }
}
