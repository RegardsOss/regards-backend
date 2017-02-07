/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Exception to be thrown when an Entity which is embedded into another one cannot be found
 *
 * This is a RuntimeException so we don't need a DTO to handle this kind of issue and we can still keep track of the
 * exception for a proper handling on the REST layer
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityEmbeddedEntityNotFoundException extends RuntimeException {

    public EntityEmbeddedEntityNotFoundException(Throwable pCause) {
        super(pCause);
    }

}