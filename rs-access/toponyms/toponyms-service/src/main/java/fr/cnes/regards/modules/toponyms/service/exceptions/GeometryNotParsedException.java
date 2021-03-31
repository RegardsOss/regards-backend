package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

public class GeometryNotParsedException extends ModuleException {


    public GeometryNotParsedException(String errorMessage) {
        super(errorMessage);
    }

    public GeometryNotParsedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeometryNotParsedException(Throwable cause) {
        super(cause);
    }

}
