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
package fr.cnes.regards.framework.modules.jobs.domain.step;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;

/**
 * Common processing step applied to job execution
 *
 * @param <I> input object
 * @param <0> output object
 * @param <R> result type
 *
 * @author Marc Sordi
 */
public abstract class AbstractProcessingStep<I, O, J extends IJob<?>> implements IProcessingStep<I, O> {

    protected final J job;

    public AbstractProcessingStep(J job) {
        this.job = job;
    }

    @Override
    public O execute(I in) throws ProcessingStepException {
        boolean error = true;
        try {
            O out = doExecute(in);
            job.advanceCompletion();
            error = false;
            return out;
        } finally {
            if (error) {
                // Always run this method even if exception occurs
                doAfterError(in);
            } else {
                //
            }
        }
    }

    /**
     * Override this method to implement step execution algorithm
     * @param in input object
     * @return output object
     * @throws ProcessingStepException
     */
    protected abstract O doExecute(I in) throws ProcessingStepException;

    /**
     * Override this method to manage step execution error
     * @param in input object
     */
    protected abstract void doAfterError(I in);
}
