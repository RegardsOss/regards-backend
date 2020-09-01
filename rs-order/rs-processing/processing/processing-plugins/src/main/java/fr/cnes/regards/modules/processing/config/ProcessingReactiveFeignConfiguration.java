package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import org.springframework.context.annotation.Configuration;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@Configuration
@EnableReactiveFeignClients(basePackageClasses = {
        IReactiveRolesClient.class,
        IReactiveStorageClient.class
})
public class ProcessingReactiveFeignConfiguration {

}
