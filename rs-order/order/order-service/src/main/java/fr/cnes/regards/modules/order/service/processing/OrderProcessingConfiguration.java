package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = IProcessingRestClient.class)
public class OrderProcessingConfiguration {
}
