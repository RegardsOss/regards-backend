/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;

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
 * Class InstanceJpaAutoConfiguration
 *
 * Configuration class to define hibernate/jpa instance database strategy
 * @author Sébastien Binda
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
