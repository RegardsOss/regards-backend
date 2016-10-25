/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ImportResource;

import fr.cnes.regards.microservices.core.annotation.MicroserviceInfo;

/**
 *
 * Start microservice ${artifactId}
 *
 * @author CS
 *
 */
// CHECKSTYLE:OFF
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules" })
// CHECKSTYLE:ON
@MicroserviceInfo(name = "administration", version = "1.0-SNAPSHOT")
@ImportResource({ "classpath*:defaultRoles.xml", "classpath*:mailSender.xml" })
@EnableDiscoveryClient
public class Application { // NOSONAR

    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }

}
