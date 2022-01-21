/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins;

import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

import java.util.List;

/**
 * Plugin that may block an acquisition processing chain execution.
 * <p>
 * This interface should only be implemented in addition to one of the acquisition plugins.
 */
public interface IChainBlockingPlugin {

    /**
     * Analyze an acquisition chain for potential blockers during execution.
     * Returns an empty list if no blocker is found.
     *
     * @param acquisitionProcessingChain The processing chain to analyze
     * @return A list of blocking issues
     */
    List<String> getExecutionBlockers(AcquisitionProcessingChain acquisitionProcessingChain);

}
