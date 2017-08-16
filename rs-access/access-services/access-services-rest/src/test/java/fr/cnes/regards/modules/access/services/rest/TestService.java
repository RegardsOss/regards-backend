/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.rest;

import java.util.Set;

import org.assertj.core.util.Sets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.catalog.services.domain.IService;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.Model;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(id = "tata", description = "plugin for test", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss", version = "1.0.0")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.QUERY }, entityTypes = { EntityType.DATA })
public class TestService implements IService {

    public static final String EXPECTED_VALUE = "skydiving";

    @PluginParameter(name = "para")
    private String para;

    @Override
    public ResponseEntity<?> apply() {
        if (!para.equals(EXPECTED_VALUE)) {
            return new ResponseEntity<Set<DataObject>>(Sets.newHashSet(), HttpStatus.OK);
        }
        Model model = Model.build("pName", "pDescription", EntityType.DATA);
        DataObject do1 = new DataObject(model, "pTenant", "pLabel1");
        DataObject do2 = new DataObject(model, "pTenant", "pLabel2");
        return new ResponseEntity<Set<DataObject>>(Sets.newLinkedHashSet(do1, do2), HttpStatus.OK);
    }

}
