/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest.contract;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountSettingsClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import fr.cnes.regards.modules.accessrights.rest.RegistrationController;

/**
 * @author Marc Sordi
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RegistrationContractIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAccountsClient accountsClient;

    @Autowired
    private IAccountSettingsClient accountSettingsClient;

    @SuppressWarnings("unchecked")
    @Test
    public void requestAccess() {

        // lets mock the feign clients responses
        Account account = new Account("sebastien.binda@c-s.fr", "seb", "seb", "seb");
        AccountSettings accountSettings = new AccountSettings();

        Mockito.when(accountsClient.retrieveAccounByEmail("sebastien.binda@c-s.fr"))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND),
                            new ResponseEntity<>(new EntityModel<>(account), HttpStatus.OK));
        AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
        Mockito.when(accountsClient.createAccount(accountNPassword))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(account), HttpStatus.CREATED));
        Mockito.when(accountSettingsClient.retrieveAccountSettings())
                .thenReturn(new ResponseEntity<>(new EntityModel<>(accountSettings), HttpStatus.OK));

        String accessRequest = readJsonContract("request-access.json");

        performDefaultPost(RegistrationController.REQUEST_MAPPING_ROOT, accessRequest,
                           customizer().expectStatusCreated(), "Access request error!");
    }

}
