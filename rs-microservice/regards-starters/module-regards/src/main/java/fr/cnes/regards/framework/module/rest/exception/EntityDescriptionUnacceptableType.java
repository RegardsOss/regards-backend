/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityDescriptionUnacceptableType extends ModuleException {

    private static final String UNACCEPTABLE_TYPE = "The system only accept description that are PDFs or Markdown. Not %s.";

    /**
     * @param pErrorMessage
     */
    public EntityDescriptionUnacceptableType(String pDescriptionType) {
        super(String.format(UNACCEPTABLE_TYPE, pDescriptionType));
    }

}
