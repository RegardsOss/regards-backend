/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityDescriptionTooLargeException extends ModuleException {

    private static final String DESCRIPTION_TOO_LARGE = "The system only accept description up to 10MB. %s do not respect this constraint.";

    /**
     * @param pDescriptionName
     */
    public EntityDescriptionTooLargeException(String pDescriptionName) {
        super(String.format(DESCRIPTION_TOO_LARGE, pDescriptionName));
    }

}
