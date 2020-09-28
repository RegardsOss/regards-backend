package fr.cnes.regards.modules.processing.config.reactive;

import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@EnableReactiveFeignClients(basePackageClasses = {
        IReactiveRolesClient.class,
        IReactiveStorageClient.class
})
public class ProcessingReactiveFeignConfiguration {

}
