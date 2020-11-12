package fr.cnes.regards.modules.indexer.dao.mapping;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;

//TODO REMOVE
@Deprecated
public interface IEsMappingCreationService {

    JsonObject createMappingForIndex(String tenant) throws ModuleException;

}
