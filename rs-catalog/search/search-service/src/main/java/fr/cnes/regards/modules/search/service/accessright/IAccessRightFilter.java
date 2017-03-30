package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * This service adds adds user group and data access filters to an ElasticSearch request.
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface IAccessRightFilter {

    /**
     * Add the current user's groups to the criterion.
     *
     * @param criterion
     * @return the passed criterion agremented of the current user's groups
     */
    ICriterion addUserGroups(ICriterion criterion);
}
