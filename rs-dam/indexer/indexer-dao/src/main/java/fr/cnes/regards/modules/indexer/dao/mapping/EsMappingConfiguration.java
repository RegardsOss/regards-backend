package fr.cnes.regards.modules.indexer.dao.mapping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;

@Configuration
public class EsMappingConfiguration {

    @Bean
    public AttrDescToJsonMapping attrDescToJsonMapping() {
        return new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE);
    }

}
