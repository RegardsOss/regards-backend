package fr.cnes.regards.modules.catalog.services.plugins.helper;

import java.util.List;

import org.springframework.data.domain.Page;

import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;

public interface IServiceHelper {

    Page<DataObject> getDataObjects(List<String> entityIds, int pageIndex, int nbEntitiesByPage);

    Page<DataObject> getDataObjects(String openSearchQuery, int pageIndex, int nbEntitiesByPage)
            throws OpenSearchParseException;

}
