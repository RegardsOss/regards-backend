package fr.cnes.regards.modules.accessRights.service.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.accessRights.dao.test",
        "fr.cnes.regards.modules.accessRights.service.test", "fr.cnes.regards.modules.accessRights.service" })
public class RoleServiceTestConfiguration {

}
