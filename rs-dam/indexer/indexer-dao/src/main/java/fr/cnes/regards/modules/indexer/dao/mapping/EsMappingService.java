package fr.cnes.regards.modules.indexer.dao.mapping;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.JsonMerger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.*;

//TODO REMOVE
@Deprecated
public class EsMappingService implements IEsMappingCreationService, IEsMappingUpdateService {

    @Autowired private IEsRepository esRepo;

    @Autowired private AttrDescToJsonMapping toMapping;

    /**
     * Attribute service
     */
    private final Supplier<Stream<AttributeDescription>> attributesAccessor;
    private JsonMerger merger = new JsonMerger();

    @VisibleForTesting
    public EsMappingService(
            IEsRepository esRepo,
            Supplier<Stream<AttributeDescription>> attributesAccessor,
            AttrDescToJsonMapping toMapping,
            JsonMerger merger
    ) {
        this.esRepo = esRepo;
        this.attributesAccessor = attributesAccessor;
        this.toMapping = toMapping;
    }


    public EsMappingService(Supplier<Stream<AttributeDescription>> attributesAccessor) {
        this.attributesAccessor = attributesAccessor;
    }

    @Override public JsonObject createMappingForIndex(String tenant) throws ModuleException {
        merger = new JsonMerger();
        return attributesAccessor.get()
            .map(attrDesc -> toMapping.toJsonMapping(attrDesc))
            .reduce(baseJsonMapping(), merger::merge, this::neverCalled);
    }


    private JsonObject baseJsonMapping() {
        return object(
            kv("dynamic_templates", array(
                object(
                    kv("doubles", object(
                        kv("match_mapping_type", "double"),
                        kv("mapping", object("type", "double")))
                    )
                )
            )),
            kv("properties", object(
                kv("type", object("type", "keyword")),
                kv("wgs84", object("type", "geo_shape")),
                kv("feature", object(
                    kv("properties", object(
                        kv("session", object("type", "keyword"))))))
            ))
        );
    }

    @Override public void addAttributeToIndexMapping(String tenant, AttributeDescription attr) throws ModuleException {
        esRepo.putMappings(tenant, attr);
    }

    private <U> U neverCalled(U one, U two) {
        throw new RsRuntimeException("Should never be called");
    }

}
