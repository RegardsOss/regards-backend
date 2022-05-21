package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

import java.util.Set;

/**
 * This service adds adds user group and data access filters to an ElasticSearch request.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessRightFilter {

    /**
     * Add the current user's groups to the criterion.
     *
     * @param criterion cannot be null !!!
     * @return the given criterion agremented with the current user's groups
     * @throws AccessRightFilterException if access rights cannot be set
     */
    ICriterion addAccessRights(ICriterion criterion) throws AccessRightFilterException;

    /**
     * Add the current user's groups that permit DATA access to the criterion.
     *
     * @param criterion cannot be null !!!
     * @return the given criterion agremented with the current user's groups that permit DATA access
     * @throws AccessRightFilterException if access rights cannot be set
     */
    ICriterion addDataAccessRights(ICriterion criterion) throws AccessRightFilterException;

    /**
     * Retrieve current user access groups or null if superuser
     *
     * @return access groups names
     * @throws AccessRightFilterException in case user has no group access
     */
    Set<String> getUserAccessGroups() throws AccessRightFilterException;
}
