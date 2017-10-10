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

import fr.cnes.regards.modules.acquisition.domain.Product;

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
     * Create a {@link MetaProduct}
     * @param label 
     * @return
     */
    public static MetaProductBuilder build(String label) {
        final MetaProduct mp = new MetaProduct();
        mp.setLabel(label);
        return new MetaProductBuilder(mp);
    }

    public MetaProductBuilder addProduct(Product product) {
        metaProduct.addProduct(product);
        return this;
    }
    
    public MetaProductBuilder addMetaFile(MetaFile metaFile) {
        metaProduct.addMetaFile(metaFile);
        return this;
    }

    public MetaProductBuilder withChecksumAlgorithm(String algo) {
        metaProduct.setChecksumAlgorithm(algo);
        return this;
    }

    public MetaProductBuilder withCleanOriginalFile(Boolean clean) {
        metaProduct.setCleanOriginalFile(clean);
        return this;
    }

    public MetaProduct get() {
        return metaProduct;
    }
}
