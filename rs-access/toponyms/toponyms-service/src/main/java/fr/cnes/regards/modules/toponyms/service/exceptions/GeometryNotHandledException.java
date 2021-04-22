package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception when an error occurred while parsing a geojson feature
 *
 * @author Iliana Ghazali
 */
public class GeometryNotHandledException extends ModuleException {

    /**
     * @param geometryType the geometry not handled
     */
    public GeometryNotHandledException(String geometryType) {
        super(String.format("The geometry type %s is not handled by REGARDS.", geometryType));
    }

    public GeometryNotHandledException(String geometryType, Throwable pCause) {
        super(String.format("The geometry type %s is not handled", geometryType), pCause);
    }

    public GeometryNotHandledException(Throwable cause) {
        super(cause);
    }

}
