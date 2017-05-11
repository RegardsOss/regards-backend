package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.controller.ProjectController;

/**
 *
 * Class DisableInstanceDaoTest
 *
 * Configuration test class for JPA instance disactivation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(basePackages = "fr.cnes.regards.framework.modules.jpa.instance",
        excludeFilters = @ComponentScan.Filter(value = ProjectController.class, type = FilterType.ASSIGNABLE_TYPE))
@EnableAutoConfiguration
@PropertySource("classpath:disable-dao.properties")
public class DisableInstanceDaoTestConfiguration {

}
