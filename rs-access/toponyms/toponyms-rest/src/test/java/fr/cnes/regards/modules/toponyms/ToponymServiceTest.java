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
package fr.cnes.regards.modules.toponyms;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

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

}
