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
package fr.cnes.regards.modules.search.client;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.urn.UniformResourceName;

/**
 * Test {@link IAccessRights} client
 * @author Marc Sordi
 *
 */
@Ignore("No elastic search index")
@TestPropertySource("classpath:test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AccessRightsClientIT extends AbstractSearchClientIT<IAccessRights> {

    @Test
    public void hasAccess() {
        ResponseEntity<Boolean> result = client
                .hasAccess(UniformResourceName.fromString("URN:AIP:DATA:CDPP:4ece80cd-7705-3ee5-babd-64c03ff61bcd:V1"));
        Assert.assertTrue(result.getBody());
    }
}
