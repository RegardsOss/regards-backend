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

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Email verification token DTO used by the REST API /accounts/{email}/verification/reset.
 *
 * @author Julien Canches
 */
public record EmailVerificationTokenDto(@Schema(description = "The token expiration date.",
                                                example = "2027-12-03T10:15:30") LocalDateTime expiryDate,
                                        @Schema(description = "The URL of the app from where the request was issued.") String originUrl,
                                        @Schema(description = "The URL to redirect the user to the account created "
                                                              + "interface.") String requestLink,
                                        @Schema(description = "The token value.") String token) {

}
