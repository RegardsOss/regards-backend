package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception when a user has reached the maximum number of toponyms added in a day
 *
 * @author Iliana Ghazali
 */
public class MaxLimitPerDayException extends ModuleException {

    public MaxLimitPerDayException(String message) {
        super(message);
    }

    public MaxLimitPerDayException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxLimitPerDayException(Throwable cause) {
        super(cause);
    }

}
