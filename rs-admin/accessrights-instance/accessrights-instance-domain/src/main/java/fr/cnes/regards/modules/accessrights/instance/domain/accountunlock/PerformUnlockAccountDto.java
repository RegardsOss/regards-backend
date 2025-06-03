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
package fr.cnes.regards.modules.accessrights.instance.domain.accountunlock;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Class PerformUnlockAccountDto
 * <p>
 * POJO for REST interface to unlock an account by using a given token
 *
 * @author Sébastien Binda
 */
public class PerformUnlockAccountDto {

    @Schema(description = "The unlock token that was provided in the email received by the user to unlock their "
                          + "account.")
    private String token = "";

    public String getToken() {
        return token;
    }

    public void setToken(final String pToken) {
        token = pToken;
    }

}
