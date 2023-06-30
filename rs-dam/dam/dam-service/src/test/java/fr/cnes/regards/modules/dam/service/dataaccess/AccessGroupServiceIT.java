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
package fr.cnes.regards.modules.dam.service.dataaccess;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RegardsTransactional
@TestPropertySource(locations = "classpath:dataaccess.properties")
//@ContextConfiguration(classes = { TestAccessGroupConfiguration.class })
//@DirtiesContext
public class AccessGroupServiceIT extends AbstractRegardsServiceTransactionalIT {

    private static final String AG1_NAME = "AG1";

    private static final String AG2_NAME = "AG2";

    private static final String AG3_NAME = "AG3";

    @Autowired
    private IAccessGroupRepository accessGroupRepository;

    @Autowired
    private IAccessGroupService accessGroupService;

    @Before
    public void init() {
        accessGroupRepository.save(new AccessGroup(AG1_NAME));

        AccessGroup accessGroup2 = new AccessGroup(AG2_NAME);
        accessGroup2.setPublic(true);
        accessGroupRepository.save(accessGroup2);

        AccessGroup accessGroup3 = new AccessGroup(AG3_NAME);
        accessGroup3.setInternal(true);
        accessGroupRepository.save(accessGroup3);
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void testCreateAccessGroupDuplicate() throws EntityAlreadyExistsException {
        // Given
        AccessGroup duplicatedAccessGroup = new AccessGroup(AG1_NAME);

        // When
        accessGroupService.createAccessGroup(duplicatedAccessGroup);
    }
    
    @Test
    public void testCreateAccessGroupByDefault() throws EntityAlreadyExistsException {
        // Given
        String accessGroupByDefault = AccessGroupService.ACCESS_GROUP_PUBLIC_DOCUMENTS;

        // When
        AccessGroup documentAccessGroup = accessGroupRepository.findOneByName(accessGroupByDefault);

        // Then
        Assert.assertNotNull(documentAccessGroup);
        Assert.assertEquals(documentAccessGroup.isPublic(), true);
    }

    @Test
    public void testRetrieveAccessGroups() {
        // When
        Page<AccessGroup> accessGroups = accessGroupService.retrieveAccessGroups(true, PageRequest.of(0, 10));
        // Then
        Assert.assertEquals(2, accessGroups.getContent().size());

        // When
        accessGroups = accessGroupService.retrieveAccessGroups(false, PageRequest.of(0, 10));
        // Then
        Assert.assertEquals(4, accessGroups.getContent().size());
    }

}
