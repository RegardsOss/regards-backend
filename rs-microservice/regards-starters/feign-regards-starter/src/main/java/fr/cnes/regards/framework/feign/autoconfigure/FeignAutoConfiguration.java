/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Feign auto configuration with profile restriction to ease mocking<br/>
 * If test profile is used, all feign clients in regards package have to be mocked.
 *
 * @author Marc Sordi
 *
 */
@Profile("!test")
@Configuration
@EnableFeignClients("fr.cnes.regards")
@AutoConfigureAfter(name = "fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration")
public class FeignAutoConfiguration {
}
