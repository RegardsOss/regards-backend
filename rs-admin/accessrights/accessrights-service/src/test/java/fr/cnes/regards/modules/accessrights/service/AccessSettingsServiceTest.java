/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettingsEvent;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.AccessSettingsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Test class for {@link AccessSettingsService}.
 *
 * @author CS SI
 */
public class AccessSettingsServiceTest {

    /**
     * Constant for auto-accept acceptance mode
     */
    private static final String AUTO_ACCEPT = "auto-accept";

    /**
     * Constant for manual acceptance mode
     */
    private static final String MANUAL = "manual";

    /**
     * The tested service
     */
    private AccessSettingsService accessSettingsService;

    /**
     * Mock repository
     */
    private IAccessSettingsRepository accessSettingsRepository;

    private IRoleRepository roleRepository;

    private IPublisher publisher;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        accessSettingsRepository = Mockito.mock(IAccessSettingsRepository.class);
        roleRepository = Mockito.mock(IRoleRepository.class);
        publisher = Mockito.mock(IPublisher.class);
        accessSettingsService = new AccessSettingsService(accessSettingsRepository, roleRepository, publisher);
    }

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to retrieve the access settings.")
    public void retrieve() {
        // Define expected
        final AccessSettings expected = new AccessSettings();
        expected.setId(0L);
        expected.setMode(MANUAL);
        expected.setDefaultRole(new Role(DefaultRole.REGISTERED_USER.toString()));
        expected.setDefaultGroups(Lists.newArrayList());

        // Mock the repository returned value
        final List<AccessSettings> asList = new ArrayList<>();
        asList.add(expected);
        Mockito.when(accessSettingsRepository.findAll()).thenReturn(asList);

        // Retrieve actual value
        final AccessSettings actual = accessSettingsService.retrieve();

        // Check that the expected and actual role have same values
        Assert.assertEquals(expected, actual);

        // Check that the repository's method was called with right arguments
        Mockito.verify(accessSettingsRepository).findAll();
    }

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system creates access settings lazily on retrieve.")
    public void findOrCreate() {
        // Define expected
        final Role role = new Role(DefaultRole.REGISTERED_USER.toString());
        final AccessSettings expected = new AccessSettings();
        expected.setId(0L);
        expected.setMode(AUTO_ACCEPT);
        expected.setDefaultRole(role);
        expected.setDefaultGroups(Lists.newArrayList());

        // Mock the repository returned value
        Mockito.when(accessSettingsRepository.findAll()).thenReturn(Lists.newArrayList());
        Mockito.when(roleRepository.findOneByName(eq(DefaultRole.REGISTERED_USER.toString()))).thenReturn(Optional.of(role));
        ArgumentCaptor<AccessSettings> capture = ArgumentCaptor.forClass(AccessSettings.class);

        // Retrieve (returns null because of mock but we're only interested in the argument capture)
        accessSettingsService.retrieve();

        // Check that the repository's method was called with right arguments
        Mockito.verify(accessSettingsRepository).findAll();
        Mockito.verify(accessSettingsRepository).save(capture.capture());

        // Check that the expected and actual role have same values
        Assert.assertEquals(expected, capture.getValue());
    }

    /**
     * Check that the system fails when trying to update a non existing access settings.
     *
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccessSettings} with passed id could not be found
     */
    @Test(expected = EntityNotFoundException.class)
    @Requirement("?")
    @Purpose("Check that the system fails when trying to update a non existing access settings.")
    public void updateEntityNotFound() throws EntityNotFoundException {
        // Define expected
        final Long id = 99L;
        final AccessSettings settings = new AccessSettings();
        settings.setId(id);
        settings.setMode(MANUAL);

        // Mock the repository returned value
        Mockito.when(accessSettingsRepository.existsById(id)).thenReturn(false);

        // Trigger the exception
        accessSettingsService.update(settings);

        // Check that the repository's method was called with right arguments
        Mockito.verify(accessSettingsRepository).existsById(id);
    }

    /**
     * Check that the system allows to update access settings in regular case.
     *
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccessSettings} with passed id could not be found
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to update access settings in regular case.")
    public void update() throws EntityNotFoundException {
        // Define expected
        final Long id = 0L;
        final Role role = new Role(DefaultRole.REGISTERED_USER.toString());
        final AccessSettings expected = new AccessSettings();
        expected.setId(id);
        expected.setMode(AUTO_ACCEPT);
        expected.setDefaultRole(new Role(DefaultRole.REGISTERED_USER.toString()));
        expected.setDefaultGroups(Lists.newArrayList());

        // Mock the repository returned value
        final List<AccessSettings> asList = new ArrayList<>();
        asList.add(expected);
        Mockito.when(accessSettingsRepository.existsById(id)).thenReturn(true);
        Mockito.when(roleRepository.findOneByName(eq(DefaultRole.REGISTERED_USER.toString()))).thenReturn(Optional.of(role));
        ArgumentCaptor<AccessSettings> capture = ArgumentCaptor.forClass(AccessSettings.class);

        // Perform the update
        accessSettingsService.update(expected);

        Mockito.verify(accessSettingsRepository).save(capture.capture());

        // Check that the expected and actual role have same values
        Assert.assertEquals(expected, capture.getValue());

        // Check that the repository's method was called with right arguments
        Mockito.verify(accessSettingsRepository).existsById(id);
        Mockito.verify(publisher).publish(new AccessSettingsEvent(expected.getMode(), expected.getDefaultRole().getName(), expected.getDefaultGroups()));
    }

}
