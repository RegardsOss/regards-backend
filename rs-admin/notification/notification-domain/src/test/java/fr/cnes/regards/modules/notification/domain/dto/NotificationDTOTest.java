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
package fr.cnes.regards.modules.notification.domain.dto;

import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Validate getter/setters on {@link NotificationDTO}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationDTOTest {

    /**
     * Message
     */
    private static final String MESSAGE = "Message";

    /**
     * Title
     */
    private static final String TITLE = "Title";

    /**
     * Title
     */
    private static final String SENDER = "send@email.com";

    /**
     * A role name
     */
    private static final String ROLE_0 = "Role0";

    /**
     * An other role name
     */
    private static final String ROLE_1 = "Role1";

    /**
     * Role recipients
     */
    private static Set<String> roleRecipients;

    /**
     * A user email
     */
    private static final String USER_0 = "user0@email.com";

    /**
     * An other user email
     */
    private static final String USER_1 = "user1@email.com";

    /**
     * User recipients
     */
    private static Set<String> projectUserRecipients;

    /**
     * Tested pojo
     */
    private NotificationDTO dto;

    /**
     * Setup a vaid notification before each test in order to unvalidate on by one
     */
    @Before
    public void setupNotification() {
        dto = new NotificationDTO();
        dto.setSender(SENDER);
        dto.setTitle(TITLE);
        dto.setMessage(MESSAGE);

        roleRecipients = new HashSet<>();
        roleRecipients.add(ROLE_0);
        roleRecipients.add(ROLE_1);
        dto.setRoleRecipients(roleRecipients);

        projectUserRecipients = new HashSet<>();
        projectUserRecipients.add(USER_0);
        projectUserRecipients.add(USER_1);
        dto.setProjectUserRecipients(projectUserRecipients);
    }

    /**
     * Check the POJO getters/setters.
     */
    @Test
    @Requirement("?")
    @Purpose("Check the POJO getters/setters.")
    public void testGettersSetters() {
        Assert.assertEquals(dto.getMessage(), MESSAGE);
        Assert.assertEquals(dto.getSender(), SENDER);
        Assert.assertEquals(dto.getTitle(), TITLE);
        Assert.assertEquals(dto.getProjectUserRecipients(), projectUserRecipients);
        Assert.assertEquals(dto.getRoleRecipients(), roleRecipients);
    }

}
