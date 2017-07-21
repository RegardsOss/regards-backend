/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * exception to be thrown when a parameter required for a job execution is not valid
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class JobParameterInvalidException extends ModuleException {

    /**
     * @param msg
     */
    public JobParameterInvalidException(String msg) {
        super(msg);
    }

    public JobParameterInvalidException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
