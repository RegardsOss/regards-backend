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

/**
 * Store tenant/number of active jobs/max number of jobs
 * 
 * @author Léo Mieulet
 */
public class JobQueue implements IJobQueue {

    /**
     * the tenant name
     */
    private final String name;

    /**
     * Number of current working job for that tenant
     */
    private int currentSize;

    /**
     * JobAllocationStrategy maintain the maximal number of thread
     */
    private final int maxSize;

    /**
     *
     * @param pName
     *            tenant name
     * @param pCurrentSize
     *            number of working job for that tenant
     * @param pMaxSize
     *            max number of job for that tenant
     */
    public JobQueue(final String pName, final int pCurrentSize, final int pMaxSize) {
        super();
        name = pName;
        currentSize = pCurrentSize;
        maxSize = pMaxSize;
    }

    /**
     * @return the tenant name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return the currentSize
     */
    @Override
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * @return the maxSize
     */
    @Override
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @param pCurrentSize
     *            the currentSize to set
     */
    @Override
    public void setCurrentSize(final int pCurrentSize) {
        currentSize = pCurrentSize;
    }

}
