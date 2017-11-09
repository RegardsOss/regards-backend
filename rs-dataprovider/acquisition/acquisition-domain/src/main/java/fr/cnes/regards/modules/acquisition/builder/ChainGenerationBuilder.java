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
     * Create a {@link ChainGeneration}
     * @param label
     * @return
     */
    public static ChainGenerationBuilder build(String label) {
        final ChainGeneration cg = new ChainGeneration();
        cg.setLabel(label);
        return new ChainGenerationBuilder(cg);
    }

    public ChainGeneration get() {
        return chain;
    }

    public ChainGenerationBuilder comment(String comment) {
        chain.setComment(comment);
        return this;
    }

    public ChainGenerationBuilder isActive() {
        chain.setActive(Boolean.TRUE);
        return this;
    }

    public ChainGenerationBuilder lastActivation(OffsetDateTime date) {
        chain.setLastDateActivation(date);
        return this;
    }

    public ChainGenerationBuilder periodicity(Long periodicity) {
        chain.setPeriodicity(periodicity);
        return this;
    }

    public ChainGenerationBuilder withMetaProduct(MetaProduct metaProduct) {
        chain.setMetaProduct(metaProduct);
        return this;
    }

    public ChainGenerationBuilder withDataSet(String dataSet) {
        chain.setDataSet(dataSet);
        return this;
    }

    public ChainGenerationBuilder withScanAcquisitionPluginConf(PluginConfiguration pluginconfId) {
        chain.setScanAcquisitionPluginConf(pluginconfId);
        return this;
    }

    public ChainGenerationBuilder withDataIngestProcessingChain(String ingesProcessing) {
        chain.setIngestProcessingChain(ingesProcessing);
        return this;
    }

}
