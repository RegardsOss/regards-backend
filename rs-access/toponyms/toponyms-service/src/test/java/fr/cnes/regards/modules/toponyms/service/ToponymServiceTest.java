/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;

/**
 *
 * @author Sébastien Binda
 *
 */
public class ToponymServiceTest extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ToponymsService service;

    @Autowired
    InstanceDaoProperties props;

    @Test
    public void findAll() throws IOException, ModuleException, URISyntaxException {
        tenantResolver.forceTenant(getDefaultTenant());
        Page<ToponymDTO> results = service.findAll(PageRequest.of(0, 100));
        Assert.assertEquals(251, results.getTotalElements());
        Assert.assertEquals(100, results.getSize());
    }

    @Test
    public void search() {

        List<ToponymDTO> toponyms = service.search("Fran", "en", 100);
        Assert.assertEquals(1, toponyms.size());

        toponyms = service.search("fr", "en", 100);
        Assert.assertEquals(6, toponyms.size());

        toponyms = service.search("e", "en", 100);
        Assert.assertEquals(100, toponyms.size());
    }

    @Test
    public void searchSimplifiedGeo() {
        Optional<ToponymDTO> result = service.findOne("France", true);
        int size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("With simplify algorithm france should contains 141 positions", 141, size);

        result = service.findOne("France", false);
        size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("Without simplify algorithm france should contains 1042 positions", 1042, size);
    }

}
