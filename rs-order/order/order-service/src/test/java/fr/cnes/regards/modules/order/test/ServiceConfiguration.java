package fr.cnes.regards.modules.order.test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;

import feign.Response;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order.service" })
@EnableAutoConfiguration
@EnableScheduling
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class ServiceConfiguration {

    @Bean
    public ICatalogClient mockCatalogClient() {
        return new CatalogClientMock();
    }

    @Bean
    public IProjectsClient mockProjectsClient() {
        return Mockito.mock(IProjectsClient.class);
    }

    @Bean
    public IAipClient mockAipClient() {
        return new IAipClient() {

            @Autowired
            private IPublisher publisher;

            @Override
            public ResponseEntity<PagedResources<Resource<AIP>>> retrieveAIPs(AIPState pState, OffsetDateTime pFrom,
                    OffsetDateTime pTo, int pPage, int pSize) {
                return null;
            }

            @Override
            public ResponseEntity<Set<UUID>> createAIP(Set<AIP> aips) {
                return null;
            }

            @Override
            public ResponseEntity<List<OAISDataObject>> retrieveAIPFiles(String s) {
                return null;
            }

            @Override
            public ResponseEntity<List<String>> retrieveAIPVersionHistory(UniformResourceName pIpId, int pPage,
                    int pSize) {
                return null;
            }

            @Override
            public ResponseEntity<AvailabilityResponse> makeFilesAvailable(AvailabilityRequest availabilityRequest) {
                for (String checksum : availabilityRequest.getChecksums()) {
                    if ((int)(Math.random() * 10) % 2 == 0) {
                        publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, checksum));
                    } else {
                        publisher.publish(new DataFileEvent(DataFileEventState.ERROR, checksum));
                    }
                }
                return ResponseEntity.ok(new AvailabilityResponse(Collections.emptySet(), Collections.emptySet(),
                                                                 Collections.emptySet()));
            }

            @Override
            public Response downloadFile(String aipId, String checksum) {
                Response mockResp = Mockito.mock(Response.class);
                try {
                    Mockito.when(mockResp.body().asInputStream()).thenReturn( getClass().getResourceAsStream("/files/" + checksum));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return mockResp;
            }
        };
    }

    @Bean
    public IAuthenticationResolver mockAuthResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }


    @Bean
    public IEmailClient mockEmailClient() {
        return Mockito.mock(IEmailClient.class);
    }
}
