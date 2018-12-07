/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.json.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.json.GsonUtil;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "fr.cnes.regards.framework" })
@PropertySource("classpath:tests.properties")
public class JsonbTestConfiguration {

    @Value("${spring.datasource.url}")
    private String jdbcurl;

    @Bean
    public Void setGsonIntoGsonUtil(Gson pGson) {
        GsonUtil.setGson(pGson);
        return null;
    }

}
