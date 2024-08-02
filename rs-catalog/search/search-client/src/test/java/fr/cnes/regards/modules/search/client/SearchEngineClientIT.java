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
package fr.cnes.regards.modules.search.client;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @author Marc Sordi
 */
@TestPropertySource("classpath:test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SearchEngineClientIT extends AbstractSearchClientIT<ISearchEngineClient> {

    @Test
    public void search() {
        ResponseEntity<?> result = client.searchAllDataobjects("legacy",
                                                               new HttpHeaders(),
                                                               new LinkedMultiValueMap<>(),
                                                               0,
                                                               10000);
        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());

        // ResponseEntity<Set<UniformResourceName>> response = client.hasAccess(Collections.emptyList());
        // Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }
}
