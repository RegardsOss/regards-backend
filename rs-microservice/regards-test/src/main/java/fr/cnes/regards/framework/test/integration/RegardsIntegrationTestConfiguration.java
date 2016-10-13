package fr.cnes.regards.framework.test.integration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@ComponentScan(basePackages = { "fr.cnes.regards" })
@ActiveProfiles("test")
public class RegardsIntegrationTestConfiguration {

}
