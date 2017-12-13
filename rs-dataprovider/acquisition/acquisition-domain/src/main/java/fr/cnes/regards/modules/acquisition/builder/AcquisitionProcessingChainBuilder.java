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
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * {@link AcquisitionProcessingChain} builder
 *
 * @author Christophe Mertz
 *
 */
public final class AcquisitionProcessingChainBuilder {

    /**
     * Current {@link AcquisitionProcessingChain}
     */
    private final AcquisitionProcessingChain chain;

    private AcquisitionProcessingChainBuilder(AcquisitionProcessingChain processingChain) {
        this.chain = processingChain;
    }

    /**
     * Create a {@link AcquisitionProcessingChainBuilder}
     * @param label the label {@link String} value
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public static AcquisitionProcessingChainBuilder build(String label) {
        final AcquisitionProcessingChain cg = new AcquisitionProcessingChain();
        cg.setLabel(label);
        return new AcquisitionProcessingChainBuilder(cg);
    }

    /**
     * Get the current {@link AcquisitionProcessingChain}
     * @return the current {@link AcquisitionProcessingChain}
     */
    public AcquisitionProcessingChain get() {
        return chain;
    }

    /**
     * Set the comment property to the current {@link AcquisitionProcessingChain}
     * @param comment
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder comment(String comment) {
        chain.setComment(comment);
        return this;
    }

    /**
     * Set the active property to the current {@link AcquisitionProcessingChain}
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder isActive() {
        chain.setActive(Boolean.TRUE);
        return this;
    }

    /**
     * Set the last acquition date property to the current {@link AcquisitionProcessingChain}
     * @param date
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder lastActivation(OffsetDateTime date) {
        chain.setLastDateActivation(date);
        return this;
    }

    /**
     * Set the periodicity property to the current {@link AcquisitionProcessingChain}
     * @param periodicity
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder periodicity(Long periodicity) {
        chain.setPeriodicity(periodicity);
        return this;
    }

    /**
     * Set the {@link MetaProduct} property to the current {@link AcquisitionProcessingChain}
     * @param metaProduct
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder withMetaProduct(MetaProduct metaProduct) {
        chain.setMetaProduct(metaProduct);
        return this;
    }

    /**
     * Set the dataset property to the current {@link AcquisitionProcessingChain}
     * @param dataSet the dataset {@link String} value
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder withDataSet(String dataSet) {
        chain.setDataSet(dataSet);
        return this;
    }

    /**
     * Set the session property to the current {@link AcquisitionProcessingChain}
     * @param session the session {@link String} value
     * @return the current {@link AcquisitionProcessingChainBuilder}
     */
    public AcquisitionProcessingChainBuilder withSession(String session) {
        chain.setSession(session);
        return this;
    }

    /**
     * Set the scan {@link PluginConfiguration} id property to the current {@link AcquisitionProcessingChain}
     * @param pluginconfId
     * @return the current {@link AcquisitionProcessingChainBuilder} 
     */
    public AcquisitionProcessingChainBuilder withScanAcquisitionPluginConf(PluginConfiguration pluginconfId) {
        chain.setScanAcquisitionPluginConf(pluginconfId);
        return this;
    }

}
