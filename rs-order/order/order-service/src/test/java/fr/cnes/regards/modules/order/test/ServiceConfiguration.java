package fr.cnes.regards.modules.order.test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ActiveProfiles;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order" })
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class ServiceConfiguration {
    @Bean
    public ICatalogClient mockCatalogClient() {
        return new CatalogClientMock();
    }

    @Bean
    public IAipClient mockAipClient() {
        return Mockito.mock(IAipClient.class);
    }

//    @Bean
//    public IAipClient aipClient() {
//        return new IAipClient() {
//
//            @Override
//            public InputStream downloadFile(String aipId, String checksum) {
//                return getClass().getResourceAsStream("/files/" + checksum);
//            }
//
//            @Override
//            public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(AIPState pState, OffsetDateTime pFrom,
//                    OffsetDateTime pTo, int pPage, int pSize) {
//                return null;
//            }
//
//            @Override
//            public HttpEntity<Set<UUID>> createAIP(Set<AIP> aips) {
//                return null;
//            }
//
//            @Override
//            public HttpEntity<List<DataObject>> retrieveAIPFiles(UniformResourceName pIpId) {
//                return null;
//            }
//
//            @Override
//            public HttpEntity<List<String>> retrieveAIPVersionHistory(UniformResourceName pIpId, int pPage,
//                    int pSize) {
//                return null;
//            }
//
//            @Override
//            public HttpEntity<AvailabilityResponse> makeFilesAvailable(AvailabilityRequest availabilityRequest) {
//                return null;
//            }
//        };
//    }
}
