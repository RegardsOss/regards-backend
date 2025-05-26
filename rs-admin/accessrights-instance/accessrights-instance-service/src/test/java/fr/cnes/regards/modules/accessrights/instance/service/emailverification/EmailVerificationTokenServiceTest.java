/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.accessrights.instance.service.emailverification;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.dao.emailverification.IEmailVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Julien Canches
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { EmailVerificationTokenService.class })
public class EmailVerificationTokenServiceTest {

    private final Account account = new Account("email", "firstName", "lastName", "password");

    @Autowired
    private EmailVerificationTokenService service;

    @MockBean
    private IEmailVerificationTokenRepository repository;

    @Test
    public void create() {
        // GIVEN
        Mockito.when(repository.save(Mockito.any())).then(AdditionalAnswers.returnsFirstArg());
        // WHEN
        EmailVerificationToken token = service.create(account, "originUrl", "requestLink");
        // THEN
        Mockito.verify(repository).save(token);
        assertThat(token.getAccount()).isSameAs(account);
        assertThat(token.getOriginUrl()).isEqualTo("originUrl");
        assertThat(token.getRequestLink()).isEqualTo("requestLink");
    }

    @Test
    public void findByToken() throws EntityNotFoundException {
        // GIVEN
        EmailVerificationToken token = new EmailVerificationToken(account, "originUrl", "requestLink");
        Mockito.when(repository.findByToken("123456")).thenReturn(Optional.of(token));
        // WHEN
        EmailVerificationToken found = service.findByToken("123456");
        // THEN
        assertThat(found).isEqualTo(token);
    }

    @Test
    public void findByToken_not_found() {
        // GIVEN
        Mockito.when(repository.findByToken("123456")).thenReturn(Optional.empty());
        // WHEN-THEN
        assertThatThrownBy(() -> service.findByToken("123456")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void findByAccount() throws EntityNotFoundException {
        // GIVEN
        EmailVerificationToken token = new EmailVerificationToken(account, "originUrl", "requestLink");
        Mockito.when(repository.findByAccount(account)).thenReturn(Optional.of(token));
        // WHEN
        EmailVerificationToken found = service.findByAccount(account);
        // THEN
        assertThat(found).isEqualTo(token);
    }

    @Test
    public void findByAccount_not_found() {
        // GIVEN
        Mockito.when(repository.findByAccount(account)).thenReturn(Optional.empty());
        // WHEN-THEN
        assertThatThrownBy(() -> service.findByAccount(account)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void delete() {
        // GIVEN
        EmailVerificationToken token = new EmailVerificationToken(account, "originUrl", "requestLink");
        Mockito.when(repository.findByAccount(account)).thenReturn(Optional.of(token));
        // WHEN
        service.deleteTokenForAccount(account);
        // THEN
        Mockito.verify(repository).delete(token);
    }

}
