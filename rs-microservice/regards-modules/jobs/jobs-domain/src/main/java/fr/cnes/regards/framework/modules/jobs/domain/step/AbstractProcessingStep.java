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
package fr.cnes.regards.framework.modules.jobs.domain.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;

import java.util.Optional;

/**
 * Common processing step applied to job execution
 * @param <I> input object
 * @param <O> output object
 * @param <J> associated processing job
 * @author Marc Sordi
 */
public abstract class AbstractProcessingStep<I, O, J extends IJob<?>> implements IProcessingStep<I, O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessingStep.class);

    protected final J job;

    public AbstractProcessingStep(J job) {
        this.job = job;
    }

    @Override
    public O execute(I in) throws ProcessingStepException {
        boolean error = true;
        Exception exception = null;
        try {
            O out = doExecute(in);
            job.advanceCompletion();
            error = false;
            return out;
        } catch (Exception e) { // NOSONAR
            LOGGER.error(e.getMessage(), e);
            exception = e;
            throw e;
        } finally {
            if (error) {
                // Always run this method even if exception occurs
                doAfterError(in, Optional.ofNullable(exception));
            }
        }
    }

    /**
     * Override this method to implement step execution algorithm
     * @param in input object
     * @return output object
     */
    protected abstract O doExecute(I in) throws ProcessingStepException;

    /**
     * Override this method to manage step execution error
     * @param in input object
     */
    protected abstract void doAfterError(I in, Optional<Exception> e);
}
