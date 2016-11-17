/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Use on update endpoint if identifier in url path doesn't match identifier in request body
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityInconsistentIdentifierException extends EntityException {

    private static final long serialVersionUID = -2244195392447606535L;

    public EntityInconsistentIdentifierException(final Long pPathId, final Long pBodyId, final Class<?> pEntityClass) {
        // CHECKSTYLE:OFF
        super(String.format(
                            "Inconsistent entity update request for \"%s\". Path identifier \"%s\" does not match request body identifier \"%s\".",
                            pEntityClass.getName(), pPathId, pBodyId));
        // CHECKSTYLE:ON
    }

}
