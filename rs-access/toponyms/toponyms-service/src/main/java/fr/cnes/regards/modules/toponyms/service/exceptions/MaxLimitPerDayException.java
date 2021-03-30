package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

public class MaxLimitPerDayException extends ModuleException {


    public MaxLimitPerDayException(String user) {
        super(String.format("The maximum number of toponyms to save is reached today for user %s. Try again tomorrow.", user));
    }

    public MaxLimitPerDayException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxLimitPerDayException(Throwable cause) {
        super(cause);
    }

}
