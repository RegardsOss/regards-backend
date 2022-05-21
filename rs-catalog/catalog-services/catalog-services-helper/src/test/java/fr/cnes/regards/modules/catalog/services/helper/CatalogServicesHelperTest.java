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
package fr.cnes.regards.modules.catalog.services.helper;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class CatalogServicesHelperTest {

    @Test
    public void searchDataObjects() throws ModuleException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("q", "id:test");
        params.add("facets", "[\"one\"]");
        params.add("sort", "param,ASC");
        SearchRequest request = new SearchRequest("legacy", null, params, null, null, null);
        Assert.assertTrue(request.hasSearchParameters());

        params.clear();
        params.add("q", "");
        params.add("facets", "[\"one\"]");
        params.add("sort", "param,ASC");
        request = new SearchRequest("legacy", null, params, null, null, null);
        Assert.assertFalse(request.hasSearchParameters());
    }

}
