package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

public interface IEsMappingUpdateService {

    void addAttributeToIndexMapping(String tenant, AttributeDescription attr) throws ModuleException;

}
