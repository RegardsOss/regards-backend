package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * This service adds adds user group and data access filters to an ElasticSearch request.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessRightFilter {

    ICriterion addGroupFilter(ICriterion criterion);

    ICriterion addAccessRightsFilter(ICriterion criterion);

    /**
     * Remove any group criterion that could be found in this criterion
     * 
     * @param pCriterion
     * @return
     */
    ICriterion removeGroupFilter(ICriterion pCriterion);
}
