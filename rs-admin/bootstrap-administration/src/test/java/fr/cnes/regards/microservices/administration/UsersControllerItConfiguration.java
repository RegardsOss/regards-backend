/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author svissier
 *
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.accessRights.dao.test",
        "fr.cnes.regards.modules.accessRights.service.test", "fr.cnes.regards.modules.accessRights.service",
        "fr.cnes.regards.modules.accessRights.dao", "fr.cnes.regards.microservices.core" })
@Profile("test")
public class UsersControllerItConfiguration {

}
