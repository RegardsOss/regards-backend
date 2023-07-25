/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.submission.creation.amqp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.concurrent.TimeUnit;

/**
 * Service to check if a user provided in {@link DeliveryRequestDtoEvent} belongs to REGARDS.
 *
 * @author Iliana Ghazali
 **/
@Service
public class UserVerifyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserVerifyService.class);

    /**
     * Cache to indicates if a given user login (email) is a valid REGARDS user or not.
     */
    private final Cache<String, Boolean> regardsUsers = Caffeine.newBuilder()
                                                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                                                .maximumSize(100)
                                                                .build();

    private final IProjectUsersClient projectUsersClient;

    public UserVerifyService(IProjectUsersClient projectUsersClient) {
        this.projectUsersClient = projectUsersClient;
    }

    /**
     * Verify if user provided in {@link DeliveryRequestDtoEvent} is already a REGARDS user. If not, its access will
     * be denied.
     *
     * @param event  with user to check
     * @param errors to store validation errors
     * @return if user access is granted
     */
    public boolean isValidUser(DeliveryRequestDtoEvent event, Errors errors) {
        Boolean validUser = false;
        String user = event.getOrder().getUser();
        if (user == null) {
            errors.rejectValue("user", DeliveryErrorType.FORBIDDEN.name(), "User should be present");
        } else {
            validUser = regardsUsers.get(user, email -> {
                FeignSecurityManager.asSystem();
                try {
                    ResponseEntity<EntityModel<ProjectUser>> response = projectUsersClient.retrieveProjectUserByEmail(
                        user);
                    return response != null && response.getStatusCode() == HttpStatus.OK;
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    LOGGER.error(e.getMessage(), e);
                    return false;
                } finally {
                    FeignSecurityManager.reset();
                }
            });
            if (validUser == null || !validUser) {
                errors.rejectValue("user", DeliveryErrorType.FORBIDDEN.name(), "Unknown user : " + user);
            } else {
                validUser = Boolean.TRUE;
            }
        }
        return Boolean.TRUE.equals(validUser);
    }
}
