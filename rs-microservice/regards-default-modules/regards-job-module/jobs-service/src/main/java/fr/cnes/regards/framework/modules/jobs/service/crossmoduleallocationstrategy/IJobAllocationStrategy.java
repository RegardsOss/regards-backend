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
package fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

/**
 * Interface for plugin JobAllocationStrategy
 * 
 * @author Léo Mieulet
 */
@FunctionalInterface
public interface IJobAllocationStrategy {

    /**
     * @param pProjects
     *            the project list
     * @param pPreviousQueueList
     *            the previous queue list
     * @param pMaxThread
     *            the number of thread allowed on the same time
     * @return see JobAllocationStrategyResponse
     */
    JobAllocationStrategyResponse getNextQueue(List<String> pProjects, List<IJobQueue> pPreviousQueueList,
            int pMaxThread);
}
