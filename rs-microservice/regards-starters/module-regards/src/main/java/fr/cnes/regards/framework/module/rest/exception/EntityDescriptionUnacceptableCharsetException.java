/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityDescriptionUnacceptableCharsetException extends ModuleException {

    private static final String MESSAGE_FORMAT = "Entity can only have description of Type: application/pdf or text/markdown;charset=utf-8 and not charset=%s";

    /**
     * @param pCharsetOfFile
     */
    public EntityDescriptionUnacceptableCharsetException(String pCharsetOfFile) {
        super(String.format(MESSAGE_FORMAT, pCharsetOfFile));
    }

}
