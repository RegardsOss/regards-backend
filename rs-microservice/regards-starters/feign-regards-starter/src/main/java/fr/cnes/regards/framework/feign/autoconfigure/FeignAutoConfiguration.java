/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Profile;

/**
 * Feign autoconfiguration with profile restriction to ease mocking<br/>
 * If test profile is used, all feign clients in regards package have to be mocked.
 *
 * @author Marc Sordi
 */
@Profile({ "production", "feign" })
@AutoConfiguration(after = GsonAutoConfiguration.class)
@EnableFeignClients("fr.cnes.regards")
public class FeignAutoConfiguration {

}
