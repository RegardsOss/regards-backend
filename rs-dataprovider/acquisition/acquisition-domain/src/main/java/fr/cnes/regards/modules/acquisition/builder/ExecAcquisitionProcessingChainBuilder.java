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

import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.ExecAcquisitionProcessingChain;

/**
 *
 * {@link ExecAcquisitionProcessingChain} builder
 *
 * @author Christophe Mertz
 *
 */
public final class ExecAcquisitionProcessingChainBuilder {

    /**
     * Current {@link ExecAcquisitionProcessingChain}
     */
    private final ExecAcquisitionProcessingChain process;

    private ExecAcquisitionProcessingChainBuilder(ExecAcquisitionProcessingChain execProcessingChain) {
        this.process = execProcessingChain;
    }

    /**
     * Create a {@link ExecAcquisitionProcessingChain}
     * @param session
     * @return the current {@link ExecAcquisitionProcessingChainBuilder}
     */
    public static ExecAcquisitionProcessingChainBuilder build(String session) {
        final ExecAcquisitionProcessingChain execProcessingChain = new ExecAcquisitionProcessingChain();
        execProcessingChain.setSession(session);
        return new ExecAcquisitionProcessingChainBuilder(execProcessingChain);
    }

    /**
     * Get the current {@link ExecAcquisitionProcessingChain}
     * @return the current {@link ExecAcquisitionProcessingChain}
     */
    public ExecAcquisitionProcessingChain get() {
        return process;
    }

    /**
     * Set the start date property to the current {@link ExecAcquisitionProcessingChain}
     * @param date
     * @return the current {@link ExecAcquisitionProcessingChainBuilder}
     */
    public ExecAcquisitionProcessingChainBuilder withStartDate(OffsetDateTime date) {
        process.setStartDate(date);
        return this;
    }

    /**
     * Set the stop date property to the current {@link ExecAcquisitionProcessingChain}
     * @param date
     * @return
     */
    public ExecAcquisitionProcessingChainBuilder withStopDate(OffsetDateTime date) {
        process.setStopDate(date);
        return this;
    }

    /**
     * Set the {@link AcquisitionProcessingChain2} property to the current {@link ExecAcquisitionProcessingChain}
     * @param processingChain
     * @return the current {@link ExecAcquisitionProcessingChainBuilder}
     */
    public ExecAcquisitionProcessingChainBuilder withChain(AcquisitionProcessingChain2 processingChain) {
        process.setChainGeneration(processingChain);
        return this;
    }
}
