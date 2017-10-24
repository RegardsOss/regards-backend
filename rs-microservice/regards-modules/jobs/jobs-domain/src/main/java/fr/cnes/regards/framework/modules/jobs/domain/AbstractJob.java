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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Map;
import java.util.Observable;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * Abstract job, all jobs must inherit this class
 * @param <R> result type
 * @author oroussel
 * @author Léo Mieulet
 */
public abstract class AbstractJob<R> extends Observable implements IJob<R> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected R result;

    /**
     * The workspace can be null, it should be cleaned after termination of a job
     */
    private Path workspace;

    /**
     * Current completion count
     */
    private int completion = 0;

    /**
     * Set the result if necessary
     * @param result the result
     */
    protected void setResult(R result) {
        this.result = result;
    }

    @Override
    public R getResult() {
        return result;
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    @Override
    public Path getWorkspace() {
        return workspace;
    }

    @Override
    public void advanceCompletion() {
        this.completion++;
        super.setChanged();
        super.notifyObservers((this.completion * 100) / getCompletionCount());
    }

    /**
     * Reject a job because <b>a parameter is missing</b>
     * @param parameterName missing parameter name
     * @throws JobParameterMissingException the related exception
     */
    protected void handleMissingParameter(String parameterName) throws JobParameterMissingException {
        String message = String.format("Missing parameter \"%s\"", parameterName);
        logger.error(message);
        throw new JobParameterMissingException(message);
    }

    /**
     * Reject a job because <b>a parameter is invalid</b>
     * @param parameter related parameter
     * @param reason reason for invalidity
     * @throws JobParameterInvalidException the related exception
     */
    protected void handleInvalidParameter(String parameterName, String reason) throws JobParameterInvalidException {
        String errorMessage = String.format("Invalid job parameter \"%s\" : \"%s\"", parameterName, reason);
        logger.error(errorMessage);
        throw new JobParameterInvalidException(errorMessage);
    }

    /**
     * Get a required non null parameter value
     * @param parameters map of parameters
     * @param parameterName parameter name to retrieve
     * @return the parameter value
     * @throws JobParameterMissingException if parameter does not exist
     * @throws JobParameterInvalidException if parameter value is null
     */
    protected <T> T getValue(Map<String, JobParameter> parameters, String parameterName)
            throws JobParameterMissingException, JobParameterInvalidException {
        JobParameter parameter = parameters.get(parameterName);
        if (parameter == null) {
            handleMissingParameter(parameterName);
        }
        if (parameter.getValue() == null) { // NOSONAR : an exception is thrown when calling handleMissingParameter
            handleInvalidParameter(parameterName, "Null value");
        }
        return parameter.getValue();
    }

    /**
     * Get an optional parameter value
     * @param parameters map of parameters
     * @param parameterName parameter name to retrieve
     * @return an {@link java.util.Optional} parameter value
     */
    protected <T> Optional<T> getOptionalValue(Map<String, JobParameter> parameters, String parameterName) {
        JobParameter parameter = parameters.get(parameterName);
        if (parameter == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(parameter.getValue());
    }
}
