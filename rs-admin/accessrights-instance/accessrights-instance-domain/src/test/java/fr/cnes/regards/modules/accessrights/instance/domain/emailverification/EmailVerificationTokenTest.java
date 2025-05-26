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
package fr.cnes.regards.modules.accessrights.instance.domain.emailverification;

import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmailVerificationToken} non-trivial methods.
 *
 * @author Julien Canches
 */
public class EmailVerificationTokenTest {

    @Test
    public void initialState() {
        // GIVEN
        Account account = new Account("mail@cnes.fr", "firstName", "lastName", "password");
        // WHEN
        EmailVerificationToken token = new EmailVerificationToken(account, "originUrl", "requestLink");
        // THEN
        assertThat(token.getAccount()).isSameAs(account);
        assertThat(token.getOriginUrl()).isSameAs("originUrl");
        assertThat(token.getRequestLink()).isSameAs("requestLink");
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(token.getExpiryDate()).isBefore(LocalDateTime.now().plusHours(72));
    }

    @Test
    public void renew() {
        // GIVEN
        Account account = new Account("mail@cnes.fr", "firstName", "lastName", "password");
        EmailVerificationToken token = new EmailVerificationToken(account, "originUrl", "requestLink");
        token.setExpiryDate(LocalDateTime.of(1900, 1, 1, 0, 0, 0, 0));
        // WHEN
        token.renew();
        // THEN
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(token.getExpiryDate()).isBefore(LocalDateTime.now().plusHours(72));
    }

}
