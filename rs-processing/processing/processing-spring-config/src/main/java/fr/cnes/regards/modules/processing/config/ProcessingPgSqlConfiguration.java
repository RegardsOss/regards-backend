/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is the configuration for postgresql.
 *
 * @author gandrieu
 */
@Configuration
public class ProcessingPgSqlConfiguration {

    @Value("${regards.processing.r2dbc.host}")
    private String r2dbcHost;

    @Value("${regards.processing.r2dbc.port}")
    private Integer r2dbcPort;

    @Value("${regards.processing.r2dbc.username}")
    private String r2dbcUsername;

    @Value("${regards.processing.r2dbc.password}")
    private String r2dbcPassword;

    @Value("${regards.processing.r2dbc.dbname}")
    private String r2dbcDbname;

    @Value("${regards.processing.r2dbc.schema:#{null}}")
    private String r2dbcSchema;

    @Value("${regards.processing.r2dbc.pool.size.min:#{1}}")
    private int poolMinSize;

    @Value("${regards.processing.r2dbc.pool.size.max:#{2}}")
    private int poolMaxSize;

    @Bean
    public PgSqlProperties r2dbcPgSqlConfig() {
        return new PgSqlProperties(r2dbcHost,
                                   r2dbcPort,
                                   r2dbcDbname,
                                   r2dbcSchema,
                                   r2dbcUsername,
                                   r2dbcPassword,
                                   poolMinSize,
                                   poolMaxSize);
    }

}
