/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * Implementation of {@link IAccessRightFilter}.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AccessRightFilter implements IAccessRightFilter {

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter#addGroupFilter(fr.cnes.regards.modules.
     * indexer.domain.criterion.ICriterion)
     */
    @Override
    public ICriterion addUserGroups(ICriterion pCriterion) {
        return pCriterion;
    }

}
