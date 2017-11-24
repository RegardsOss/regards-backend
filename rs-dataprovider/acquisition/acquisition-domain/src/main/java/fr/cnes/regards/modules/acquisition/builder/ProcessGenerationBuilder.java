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

import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ProcessGeneration;

/**
 *
 * {@link ProcessGeneration} builder
 *
 * @author Christophe Mertz
 *
 */
public final class ProcessGenerationBuilder {

    /**
     * Current {@link ProcessGeneration}
     */
    private final ProcessGeneration process;

    private ProcessGenerationBuilder(ProcessGeneration processGeneration) {
        this.process = processGeneration;
    }

    /**
     * Create a {@link ProcessGeneration}
     * @param session
     * @return
     */
    public static ProcessGenerationBuilder build(String session) {
        final ProcessGeneration processGeneration = new ProcessGeneration();
        processGeneration.setSession(session);
        return new ProcessGenerationBuilder(processGeneration);
    }

    public ProcessGeneration get() {
        return process;
    }

    public ProcessGenerationBuilder withStartDate(OffsetDateTime date) {
        process.setStartDate(date);
        return this;
    }

    public ProcessGenerationBuilder withStopDate(OffsetDateTime date) {
        process.setStopDate(date);
        return this;
    }

    public ProcessGenerationBuilder withChain(ChainGeneration chainGeneration) {
        process.setChainGeneration(chainGeneration);
        return this;
    }
}
