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
package fr.cnes.regards.modules.accessrights.domain.emailverification;

import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * @author Thibaud Michaudel
 **/
public class EmailVerificationDto {

    @Valid
    @NotBlank
    @Length(max = 128)
    @Email
    private String email;

    @Valid
    @NotBlank
    @Length(max = 128)
    private String originUrl;

    @Valid
    @NotBlank
    @Length(max = 128)
    private String requestLink;

    public EmailVerificationDto(String email, String originUrl, String requestLink) {
        this.email = email;
        this.originUrl = originUrl;
        this.requestLink = requestLink;
    }

    public String getEmail() {
        return email;
    }

    public EmailVerificationDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public EmailVerificationDto setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
        return this;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public EmailVerificationDto setRequestLink(String requestLink) {
        this.requestLink = requestLink;
        return this;
    }
}
