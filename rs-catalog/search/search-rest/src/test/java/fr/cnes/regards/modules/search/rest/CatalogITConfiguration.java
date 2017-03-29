/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Configuration
public class CatalogITConfiguration {

    @Bean
    public IDatasetClient datasetClient() {
        IDatasetClient client = Mockito.mock(IDatasetClient.class);
        Mockito.when(client.retrieveDataset(1L))
                .thenReturn(new ResponseEntity<Resource<Dataset>>(HateoasUtils.wrap(new Dataset()), HttpStatus.OK));
        return client;
    }

    @Bean
    public IAttributeModelClient attributeModelClient() {
        return Mockito.mock(IAttributeModelClient.class);
    }

    @Bean
    public IAccessRightFilter accessRightFilter() {
        return Mockito.mock(IAccessRightFilter.class);
    }

    @Bean
    public IPoller poller() {
        return Mockito.mock(IPoller.class);
    }
}
