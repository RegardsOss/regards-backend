/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * If entity not found regarding its identifier
 *
 * @author Marc Sordi
 *
 */
public class ModuleEntityNotFoundException extends ModuleException {

    private static final long serialVersionUID = 8218291903574163437L;

    public ModuleEntityNotFoundException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity of type \"%s\" with id \"%s\" not found.", pEntityClass.getName(),
                            pEntityIdentifier));
    }

}
