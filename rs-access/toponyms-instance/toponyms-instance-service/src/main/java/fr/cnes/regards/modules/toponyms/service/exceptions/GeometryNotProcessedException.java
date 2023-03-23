package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception when an error occurred while parsing a geojson feature
 *
 * @author Iliana Ghazali
 */
public class GeometryNotProcessedException extends ModuleException {

    public GeometryNotProcessedException(String errorMessage) {
        super(errorMessage);
    }

    public GeometryNotProcessedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeometryNotProcessedException(Throwable cause) {
        super(cause);
    }

}
