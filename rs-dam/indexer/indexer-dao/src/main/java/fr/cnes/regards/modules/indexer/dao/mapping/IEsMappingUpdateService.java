package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

//TODO REMOVE
@Deprecated
public interface IEsMappingUpdateService {

    void addAttributeToIndexMapping(String tenant, AttributeDescription attr) throws ModuleException;

}
