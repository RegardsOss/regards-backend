/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.jobs.domain.function.CheckedSupplier;

/**
 * Interface for all regards jobs
 * @param <R> result type
 * @author Léo Mieulet
 * @author oroussel
 */
public interface IJob<R> extends Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(IJob.class);

    /**
     * Manage job result.
     * <br/>
     * <b>Override this method for the result to be stored in its {@link JobInfo}</b>
     * @return The Job result
     */
    default R getResult() {
        return null;
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @return does the job need a workspace ?
     */
    default boolean needWorkspace() {
        return false;
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @param workspaceSupplier workspace supplier that is also called by the method to set workspace path
     */
    void setWorkspace(CheckedSupplier<Path, IOException> workspaceSupplier) throws JobWorkspaceException;

    Path getWorkspace();

    /**
     * Manage job parameters (if there are any). Job implementation has <b>to check
     * if all needed parameters are specified</b> and <b>store them</b> for use in job execution.
     * <br/>
     * <b>Beware : do nothing by default, this method must be overridden.</b>
     * @param parameters non null parameter map
     */
    default void setParameters(Map<String, JobParameter> parameters) // NOSONAR
            throws JobParameterMissingException, JobParameterInvalidException {
    }

    /**
     * To manage completion estimated date and percentComplete property, a job should provide the number of times it
     * will call {@link #advanceCompletion()} during its execution.
     * @return 100 by default
     */
    default int getCompletionCount() {
        return 100; // NOSONAR
    }

    /**
     * Advance completion count. This method should not be called more than {@link #getCompletionCount()} times
     */
    void advanceCompletion();

    /**
     * Reject a job because <b>a parameter is missing</b>
     * @param parameterName missing parameter name
     * @throws JobParameterMissingException the related exception
     */
    static void handleMissingParameter(String parameterName) throws JobParameterMissingException {
        String message = String.format("Missing parameter \"%s\"", parameterName);
        LOGGER.error(message);
        throw new JobParameterMissingException(message);
    }

    /**
     * Reject a job because <b>a parameter is invalid</b>
     * @param parameterName related parameter
     * @param reason reason for invalidity
     * @throws JobParameterInvalidException the related exception
     */
    static void handleInvalidParameter(String parameterName, String reason) throws JobParameterInvalidException {
        String errorMessage = String.format("Invalid job parameter \"%s\" : \"%s\"", parameterName, reason);
        LOGGER.error(errorMessage);
        throw new JobParameterInvalidException(errorMessage);
    }

    /**
     * Reject a job because <b>a parameter is invalid</b>
     * @param parameterName related parameter
     * @param reason reason for invalidity
     * @throws JobParameterInvalidException the related exception
     */
    static void handleInvalidParameter(String parameterName, Exception reason) throws JobParameterInvalidException {
        String errorMessage = String.format("Invalid job parameter \"%s\" : \"%s\"", parameterName,
                                            reason.getMessage());
        LOGGER.error(errorMessage, reason);
        throw new JobParameterInvalidException(errorMessage);
    }

    /**
     * Get a required non null parameter value
     * @param parameters map of parameters
     * @param parameterName parameter name to retrieve
     * @param type to return (may be guessed for simple type, use {@link TypeToken#getType()} instead)
     * @return the parameter value
     * @throws JobParameterMissingException if parameter does not exist
     * @throws JobParameterInvalidException if parameter value is null
     */
    static <T> T getValue(Map<String, JobParameter> parameters, String parameterName, Type type)
            throws JobParameterMissingException, JobParameterInvalidException {
        JobParameter parameter = parameters.get(parameterName);
        if (parameter == null) {
            handleMissingParameter(parameterName);
        } else if (parameter.getValue() == null) { // NOSONAR : an exception is thrown when calling handleMissingParameter
            handleInvalidParameter(parameterName, "Null value");
        } else {
            return type == null ? parameter.getValue() : parameter.getValue(type);
        }
        // Unreachable code (handle... methods throw Exceptions)
        return null;
    }

    static <T> T getValue(Map<String, JobParameter> parameters, String parameterName)
            throws JobParameterMissingException, JobParameterInvalidException {
        return getValue(parameters, parameterName, null);
    }

    /**
     * Get parameter value as an Optional
     * @param parameters map of parameters
     * @param parameterName parameter name to retrieve
     * @param type to return (may be guessed for simple type, use {@link TypeToken#getType()} instead)
     * @return an {@link java.util.Optional} parameter value
     */
    static <T> Optional<T> getOptionalValue(Map<String, JobParameter> parameters, String parameterName, Type type) {
        JobParameter parameter = parameters.get(parameterName);
        if (parameter == null) {
            return Optional.empty();
        }
        T val = type == null ? parameter.getValue() : parameter.getValue(type);
        return Optional.ofNullable(val);
    }

    static <T> Optional<T> getOptionalValue(Map<String, JobParameter> parameters, String parameterName) {
        return getOptionalValue(parameters, parameterName, null);
    }
}
