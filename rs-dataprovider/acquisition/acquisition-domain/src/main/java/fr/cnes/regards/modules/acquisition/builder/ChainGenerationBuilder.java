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

import java.time.OffsetDateTime;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * {@link ChainGeneration} builder
 *
 * @author Christophe Mertz
 *
 */
public final class ChainGenerationBuilder {

    /**
     * Current {@link ChainGeneration}
     */
    private final ChainGeneration chain;

    private ChainGenerationBuilder(ChainGeneration chainGeneration) {
        this.chain = chainGeneration;
    }

    /**
     * Create a {@link ChainGenerationBuilder}
     * @param label the label {@link String} value
     * @return the current {@link ChainGenerationBuilder}
     */
    public static ChainGenerationBuilder build(String label) {
        final ChainGeneration cg = new ChainGeneration();
        cg.setLabel(label);
        return new ChainGenerationBuilder(cg);
    }

    /**
     * Get the current {@link ChainGeneration}
     * @return the current {@link ChainGeneration}
     */
    public ChainGeneration get() {
        return chain;
    }

    /**
     * Set the comment property to the current {@link ChainGeneration}
     * @param comment
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder comment(String comment) {
        chain.setComment(comment);
        return this;
    }

    /**
     * Set the active property to the current {@link ChainGeneration}
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder isActive() {
        chain.setActive(Boolean.TRUE);
        return this;
    }

    /**
     * Set the last acquition date property to the current {@link ChainGeneration}
     * @param date
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder lastActivation(OffsetDateTime date) {
        chain.setLastDateActivation(date);
        return this;
    }

    /**
     * Set the periodicity property to the current {@link ChainGeneration}
     * @param periodicity
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder periodicity(Long periodicity) {
        chain.setPeriodicity(periodicity);
        return this;
    }

    /**
     * Set the {@link MetaProduct} property to the current {@link ChainGeneration}
     * @param metaProduct
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder withMetaProduct(MetaProduct metaProduct) {
        chain.setMetaProduct(metaProduct);
        return this;
    }

    /**
     * Set the dataset property to the current {@link ChainGeneration}
     * @param dataSet the dataset {@link String} value
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder withDataSet(String dataSet) {
        chain.setDataSet(dataSet);
        return this;
    }

    /**
     * Set the session property to the current {@link ChainGeneration}
     * @param session the session {@link String} value
     * @return the current {@link ChainGenerationBuilder}
     */
    public ChainGenerationBuilder withSession(String session) {
        chain.setSession(session);
        return this;
    }

    /**
     * Set the scan {@link PluginConfiguration} id property to the current {@link ChainGeneration}
     * @param pluginconfId
     * @return the current {@link ChainGenerationBuilder} 
     */
    public ChainGenerationBuilder withScanAcquisitionPluginConf(PluginConfiguration pluginconfId) {
        chain.setScanAcquisitionPluginConf(pluginconfId);
        return this;
    }

}
