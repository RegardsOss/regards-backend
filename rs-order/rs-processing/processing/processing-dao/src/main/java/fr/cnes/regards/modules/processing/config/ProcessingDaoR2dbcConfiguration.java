/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.dao.*;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.converter.DaoCustomConverters;
import fr.cnes.regards.modules.processing.entity.mapping.BatchMapper;
import fr.cnes.regards.modules.processing.utils.gson.ProcessingGsonUtils;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import name.nkonev.r2dbc.migrate.core.Dialect;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrate;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.time.Duration;
import java.util.Collections;

import static io.r2dbc.pool.PoolingConnectionFactoryProvider.ACQUIRE_RETRY;
import static io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME;
import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcRepositories(basePackageClasses = {
        IBatchEntityRepository.class,
        IExecutionEntityRepository.class,
        IOutputFileEntityRepository.class
})
@EnableAutoConfiguration(exclude = {
        R2dbcMigrateAutoConfiguration.class
})
@EntityScan(basePackageClasses = { BatchEntity.class, ExecutionEntity.class })
@ComponentScan(basePackageClasses = {
        BatchMapper.class,
        PBatchRepositoryImpl.class,
        PExecutionRepositoryImpl.class
})
public class ProcessingDaoR2dbcConfiguration extends AbstractR2dbcConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingDaoR2dbcConfiguration.class);
    @Autowired private final PgSqlProperties pgSqlProperties;
    @Autowired @Qualifier("gson") private final Gson gson;

    public ProcessingDaoR2dbcConfiguration(PgSqlProperties pgSqlProperties, Gson gson) {
        this.pgSqlProperties = pgSqlProperties;
        this.gson = gson;
    }

    @Bean public ConnectionFactory connectionFactory() {
        Builder builder = builder()
                .option(DRIVER, "pool")
                .option(ACQUIRE_RETRY, 5)
                .option(MAX_ACQUIRE_TIME, Duration.ofSeconds(5))
                .option(PROTOCOL, "postgresql")
                .option(HOST, pgSqlProperties.getHost())
                .option(PORT, pgSqlProperties.getPort())
                .option(DATABASE, pgSqlProperties.getDbname())
                .option(SCHEMA, pgSqlProperties.getSchema());
        if (pgSqlProperties.getUser() != null) {
               builder = builder.option(USER, pgSqlProperties.getUser());
        }
        if (pgSqlProperties.getPassword() != null) {
               builder = builder.option(PASSWORD, pgSqlProperties.getPassword());
        }
        return ConnectionFactories.get(builder.build());
    }

    protected java.util.List<Object> getCustomConverters() {
        return DaoCustomConverters.getCustomConverters(gson);
    }


    @Bean(name = "r2dbcDaoTransactionManager")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public R2dbcTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }


    public static class R2dbcMigrateBlockingInvoker {
        private ConnectionFactory connectionFactory;
        private R2dbcMigrateProperties properties;

        public R2dbcMigrateBlockingInvoker(ConnectionFactory connectionFactory,
                R2dbcMigrateProperties properties) {
            this.connectionFactory = connectionFactory;
            this.properties = properties;
        }

        public void migrate() {
            LOGGER.info("Starting R2DBC migration");
            R2dbcMigrate.migrate(connectionFactory, properties).block();
            LOGGER.info("End of R2DBC migration");
        }

    }

    @Bean
    public R2dbcMigrateProperties r2dbcMigrateProperties() {
        R2dbcMigrateProperties props = new R2dbcMigrateProperties();
        props.setDialect(Dialect.POSTGRESQL);
        props.setResourcesPaths(Collections.singletonList("classpath:/db/migrations/*.sql"));
        return props;
    }

    @Bean(initMethod = "migrate")
    public R2dbcMigrateBlockingInvoker r2dbcMigrate(
            ConnectionFactory connectionFactory,
            R2dbcMigrateProperties properties
    ) {
        return new R2dbcMigrateBlockingInvoker(connectionFactory, properties);
    }

}
