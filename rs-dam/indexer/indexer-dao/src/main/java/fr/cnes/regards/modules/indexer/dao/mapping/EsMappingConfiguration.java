package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.domain.event.AttributeModelUpdated;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;
import java.util.stream.Stream;

@Configuration
public class EsMappingConfiguration {

    @Bean
    public EsMappingService esMappingUpdater(IAttributeModelRepository attrRepo) {
        Supplier<Stream<AttributeDescription>> getAllAttrDescs =
                () -> attrRepo.findAll().stream()
                    .map(AttributeModelUpdated::new)
                    .map(AttributeDescription::new);

        return new EsMappingService(getAllAttrDescs);
    }

    @Bean
    public AttrDescToJsonMapping attrDescToJsonMapping() {
        return new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE);
    }

}
