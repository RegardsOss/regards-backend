package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.controller.DaoTestController;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.service.DaoUserService;

/**
 *
 * Class DisableInstanceDaoTest
 *
 * Configuration test class for JPA multitenant disactivation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(basePackages = "fr.cnes.regards.framework.jpa.multitenant",
        excludeFilters = { @ComponentScan.Filter(value = DaoTestController.class, type = FilterType.ASSIGNABLE_TYPE),
                @ComponentScan.Filter(value = DaoUserService.class, type = FilterType.ASSIGNABLE_TYPE) })
@PropertySource("classpath:disable-dao.properties")
public class DisableMultitenantDaoTestConfiguration {

}
