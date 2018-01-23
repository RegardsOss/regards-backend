package fr.cnes.regards.modules.catalog.services.plugins.helper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;

@Service
@MultitenantTransactional
public class ServiceHelper implements IServiceHelper {

    @Autowired
    private ISearchService searchService;

    @Autowired
    private IOpenSearchService openSearchService;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Override
    public Page<DataObject> getDataObjects(List<String> entityIds, int pageIndex, int nbEntitiesByPage) {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA);
        ICriterion[] idCrits = new ICriterion[entityIds.size()];
        int count = 0;
        for (String id : entityIds) {
            idCrits[count] = ICriterion.eq("ipId", id);
            count++;
        }
        PageRequest pageReq = new PageRequest(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, ICriterion.or(idCrits));
    }

    @Override
    public Page<DataObject> getDataObjects(String openSearchQuery, int pageIndex, int nbEntitiesByPage)
            throws OpenSearchParseException {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA);
        ICriterion crit = openSearchService.parse(openSearchQuery);
        PageRequest pageReq = new PageRequest(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, crit);
    }

}
