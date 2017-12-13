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

import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * {@link AcquisitionProcessingChain} builder
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
     * Create a {@link ProductBuilder}
     * @param name the {@link Product} {@link String} value
     * @return the current {@link ProductBuilder}
     */
    public static ProductBuilder build(String name) {
        final Product pr = new Product();
        pr.setProductName(name);
        return new ProductBuilder(pr);
    }

    /**
     * Get the current {@link Product}
     * @return the current {@link Product}
     */
    public Product get() {
        return product;
    }

    /**
     * Set the status property to the current {@link Product}
     * @param status the status {@link String} value
     * @return the current {@link ProductBuilder}
     */
    public ProductBuilder withStatus(ProductStatus status) {
        product.setStatus(status);
        return this;
    }

    /**
     * Set the {@link MetaProduct} property to the current {@link Product}
     * @param metaProduct
     * @return the current {@link ProductBuilder}
     */
    public ProductBuilder withMetaProduct(MetaProduct metaProduct) {
        product.setMetaProduct(metaProduct);
        return this;
    }

    /**
     * Set the session property to the current {@link Product}
     * @param session the session {@link String} value
     * @return the current {@link ProductBuilder}
     */
    public ProductBuilder withSession(String session) {
        product.setSession(session);
        return this;
    }

    /**
     * Set the ingest processing chain property to the current {@link Product}
     * @param ingestChain the ingest processing chain {@link String} value
     * @return the current {@link ProductBuilder}
     */
    public ProductBuilder withIngestProcessingChain(String ingestChain) {
        product.setIngestChain(ingestChain);
        return this;
    }

    /**
     * Set the sended property to the current {@link Product}
     * @param sended
     * @return the current {@link ProductBuilder}
     */
    public ProductBuilder isSended(boolean sended) {
        product.setSended(sended);
        return this;
    }

}
