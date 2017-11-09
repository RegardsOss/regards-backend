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

import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * {@link ChainGeneration} builder
 *
 * @author Christophe Mertz
 *
 */
public final class ProductBuilder {

    /**
     * Current {@link Product}
     */
    private final Product product;

    private ProductBuilder(Product aProduct) {
        this.product = aProduct;
    }

    /**
     * Create a {@link Product}
     * @param name
     * @return
     */
    public static ProductBuilder build(String name) {
        final Product pr = new Product();
        pr.setProductName(name);
        return new ProductBuilder(pr);
    }

    public Product get() {
        return product;
    }

    public ProductBuilder withStatus(String status) {
        product.setStatus(ProductStatus.valueOf(status));
        return this;
    }

    public ProductBuilder withMetaProduct(MetaProduct metaProduct) {
        product.setMetaProduct(metaProduct);
        return this;
    }

}
