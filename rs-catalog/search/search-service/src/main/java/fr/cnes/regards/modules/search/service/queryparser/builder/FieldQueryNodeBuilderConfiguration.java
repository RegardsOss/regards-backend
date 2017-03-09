/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser.builder;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * Spring configuration bean for {@link FieldQueryNodeBuilder}.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class FieldQueryNodeBuilderConfiguration {

    /**
     * Feign client for rs-dam {@link AttributeModel} controller
     */
    // private final IAttributeModelClient attributeModelClient;

    @Bean
    public List<AttributeModel> attributeModels() {
        return Lists.newArrayList(new AttributeModel());
    }

}
