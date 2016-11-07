/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IRoleSysAccessVoter;

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
@EnableScheduling
public class Application { // NOSONAR

    public static void main(final String[] pArgs) {
        SpringApplication.run(Application.class, pArgs); // NOSONAR
    }

    /**
     *
     * Specific method authorization voter to accept systems roles for all endpoints.
     *
     * @return IRoleSysAccessVoter
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IRoleSysAccessVoter roleSysVoter() {
        return new AcceptRoleSysVoter();
    }

    /**
     *
     * Specific method authorization voter to accept instance admin user for all endpoints.
     *
     * @return IRoleSysAccessVoter
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IInstanceAdminAccessVoter instanceAdminVoter(
            @Value("${regards.accounts.root.user.login}") final String pInstanceUser) {
        return new AcceptInstanceAdminVoter(pInstanceUser);
    }

}
