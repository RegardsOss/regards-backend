/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Use on update endpoint if identifier in url path doesn't match identifier in request body
 *
 * @author Marc Sordi
 *
 */
public class ModuleInconsistentEntityIdentifierException extends ModuleException {

    private static final long serialVersionUID = -2244195392447606535L;

    public ModuleInconsistentEntityIdentifierException(final Long pPathId, final Long pBodyId,
            final Class<?> pEntityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                            "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                            pEntityClass.getName(), pPathId, pBodyId));
        // CHECKSTYLE:ON
    }

}
