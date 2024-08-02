/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.lang.annotation.Annotation;

/**
 * Class InstanceJpaAutoConfiguration
 * <p>
 * Configuration class to define hibernate/jpa instance database strategy This class use @InstanceEntity annotation to
 * find JPA Entities and Repositories.
 *
 * @author Sébastien Binda
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class, after = GsonAutoConfiguration.class)
@Conditional(value = EnableInstanceCondition.class)
@EnableJpaRepositories(includeFilters = { @ComponentScan.Filter(value = InstanceEntity.class,
                                                                type = FilterType.ANNOTATION) },
                       basePackages = DaoUtils.ROOT_PACKAGE,
                       entityManagerFactoryRef = "instanceEntityManagerFactory",
                       transactionManagerRef = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties(InstanceDaoProperties.class)
@ConditionalOnProperty(prefix = "regards.jpa", name = "instance.enabled", matchIfMissing = true)
public class InstanceJpaAutoConfiguration extends AbstractJpaAutoConfiguration {

    public InstanceJpaAutoConfiguration() throws MultiDataBasesException {
        super();
    }

    @Override
    public Class<? extends Annotation> getEntityAnnotationScan() {
        return InstanceEntity.class;
    }
}
