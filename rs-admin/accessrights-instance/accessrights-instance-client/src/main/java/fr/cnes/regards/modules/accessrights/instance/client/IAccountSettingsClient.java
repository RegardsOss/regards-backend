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
package fr.cnes.regards.modules.accessrights.instance.client;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 * Feign client for rs-admin Accounts controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(path = "/accounts/settings", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAccountSettingsClient {

    /**
     * Retrieve the {@link AccountSettings} for the instance.
     *
     * @return The {@link AccountSettings} wrapped in a {@link Resource} and a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<Resource<AccountSettings>> retrieveAccountSettings();

    /**
     * Update the {@link AccountSettings} for the instance.
     *
     * @param pSettings
     *            The {@link AccountSettings}
     * @return The updated account settings
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    ResponseEntity<Void> updateAccountSetting(@Valid @RequestBody AccountSettings pUpdatedAccountSetting);

}
