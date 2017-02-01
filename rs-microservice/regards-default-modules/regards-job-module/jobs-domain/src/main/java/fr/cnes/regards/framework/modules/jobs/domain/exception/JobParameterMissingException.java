/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception to be thrown when a parameter required for a job execution is missing
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class JobParameterMissingException extends ModuleException {

    /**
     * @param pErrorMessage
     */
    public JobParameterMissingException(String pErrorMessage) {
        super(pErrorMessage);
    }

    /**
     * @param pCause
     */
    public JobParameterMissingException(Throwable pCause) {
        super(pCause);
    }

}
