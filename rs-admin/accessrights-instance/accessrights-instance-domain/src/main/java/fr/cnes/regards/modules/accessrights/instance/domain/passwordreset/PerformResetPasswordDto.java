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
package fr.cnes.regards.modules.accessrights.instance.domain.passwordreset;

/**
 * Dto class
 *
 * @author Xavier-Alexandre Brochard
 */
public class PerformResetPasswordDto {

    /**
     * The token
     */
    private String token;

    /**
     * The new password
     */
    private String newPassword;

    /**
     * @param pToken
     * @param pNewPassword
     */
    public PerformResetPasswordDto(final String pToken, final String pNewPassword) {
        super();
        token = pToken;
        newPassword = pNewPassword;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param pToken the token to set
     */
    public void setToken(final String pToken) {
        token = pToken;
    }

    /**
     * @return the newPassword
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * @param pNewPassword the newPassword to set
     */
    public void setNewPassword(final String pNewPassword) {
        newPassword = pNewPassword;
    }

}
