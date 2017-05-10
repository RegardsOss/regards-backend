package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.controller.DaoTestController;

/**
 *
 * Class DisableInstanceDaoTest
 *
 * Configuration test class for JPA multitenant disactivation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ComponentScan(basePackages = "fr.cnes.regards.framework.modules.jpa.multitenant",
        excludeFilters = { @ComponentScan.Filter(value = DaoTestController.class, type = FilterType.ASSIGNABLE_TYPE),
                @ComponentScan.Filter(value = DaoUserService.class, type = FilterType.ASSIGNABLE_TYPE) })
@PropertySource("classpath:disable-dao.properties")
public class DisableMultitenantDaoTestConfiguration {

}
