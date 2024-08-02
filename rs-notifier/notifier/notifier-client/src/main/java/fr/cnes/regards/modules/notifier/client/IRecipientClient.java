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
package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.notifier.dto.RecipientDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * Feign client exposing the rs-notifier endpoints(REST requests) to other microservices plugged through Eureka.
 *
 * @author Stephane Cortine
 */
@RestClient(name = "rs-notifier", contextId = "rs-access-project.notifier-client")
public interface IRecipientClient {

    String RECIPIENTS_ROOT_PATH = "/recipients";

    /**
     * Retrieve all recipient(missing parameter) or only recipients enabling the direct notification or
     * only them not enabling the direct notification
     *
     * @param directNotificationEnabled Recipient enable or not the direct notification(not required)
     * @return list of recipients {@link RecipientDto}
     */
    @GetMapping(value = IRecipientClient.RECIPIENTS_ROOT_PATH,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Set<RecipientDto>> findRecipients(
        @RequestParam(value = "directNotificationEnabled", required = false) final Boolean directNotificationEnabled);

}
