package fr.cnes.regards.microservices.core.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@ComponentScan(basePackages = { "fr.cnes.regards" })
@ActiveProfiles("test")
public class RegardsIntegrationTestConfiguration {

}
