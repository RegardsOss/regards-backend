/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Class EntityNotFoundException
 *
 * Exception to indicates that the required entity is not found.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 *
 */
public class EntityNotFoundException extends EntityException {

    public EntityNotFoundException(final String pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity %s with id : %s doesn't exists", pEntityClass.getName(), pEntityIdentifier));
    }

    public EntityNotFoundException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        this(String.valueOf(pEntityIdentifier), pEntityClass);
    }

    /**
     * @param pEntityId
     */
    public EntityNotFoundException(Long pEntityIdentifier) {
        this(String.valueOf(pEntityIdentifier));
    }

    /**
     * @param pEntityIpId
     */
    public EntityNotFoundException(String pEntityIpId) {
        super(String.format("Entity with ipId : %s doesn't exists", pEntityIpId));
    }
}
