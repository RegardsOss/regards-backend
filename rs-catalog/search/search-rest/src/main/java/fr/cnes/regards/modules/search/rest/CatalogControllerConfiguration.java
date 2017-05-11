/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
public class CatalogControllerConfiguration {

    @Autowired
    HateoasPageableHandlerMethodArgumentResolver pageableResolver;

    /**
     * Custom resources assembler for pages of {@link AbstractEntity} with facets
     * @return the bean
     */
    @Bean
    public FacettedPagedResourcesAssembler<AbstractEntity> abstractEntityResourcesAssembler() {
        return new FacettedPagedResourcesAssembler<>(pageableResolver, null);
    }

    /**
     * Custom resources assembler for pages of {@link DataObject} with facets
     * @return the bean
     */
    @Bean
    public FacettedPagedResourcesAssembler<DataObject> dataobjectResourcesAssembler() {
        return new FacettedPagedResourcesAssembler<>(pageableResolver, null);
    }

}
