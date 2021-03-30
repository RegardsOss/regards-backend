package fr.cnes.regards.modules.toponyms.service.exceptions;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

public class GeometryNotHandledException extends ModuleException {


    /**
     * @param geometryType the message
     */
    public GeometryNotHandledException(String geometryType) {
        super(String.format("The geometry type %s is not handled by REGARDS. The Toponym was not saved.", geometryType));
    }

    /**
     * @param pCause the caught exception which triggered this exception
     */
    public GeometryNotHandledException(String geometryType, Throwable pCause) {
        super(String.format("The geometry type %s is not handled", geometryType), pCause);
    }

    public GeometryNotHandledException(Throwable cause) {
        super(cause);
    }

}
