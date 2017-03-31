/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import java.lang.annotation.Annotation;

import javax.persistence.Entity;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;

/**
 *
 * Class InstanceJpaAutoConfiguration
 *
 * Configuration class to define hibernate/jpa instance database strategy
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@Conditional(value = DisableInstanceCondition.class)
@EnableJpaRepositories(basePackages = DaoUtils.ROOT_PACKAGE, entityManagerFactoryRef = "instanceEntityManagerFactory",
        transactionManagerRef = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties(InstanceDaoProperties.class)
@ConditionalOnProperty(prefix = "regards.jpa", name = "instance.enabled", matchIfMissing = true)
@AutoConfigureAfter(value = { GsonAutoConfiguration.class })
public class JpaAutoConfiguration extends AbstractJpaAutoConfiguration {

    public JpaAutoConfiguration() throws MultiDataBasesException {
        super();
    }

    @Override
    public Class<? extends Annotation> getEntityAnnotationScan() {
        return Entity.class;
    }

}
