package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsMappingConfiguration {

    @Bean
    public AttrDescToJsonMapping attrDescToJsonMapping() {
        return new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE);
    }

}
