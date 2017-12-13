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
package fr.cnes.regards.modules.catalog.services.rest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashSet;

import org.assertj.core.util.Sets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.models.domain.Model;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(id = "tata", description = "plugin for test", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss", version = "1.0.0")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class TestService implements IService {

    public static final String EXPECTED_VALUE = "skydiving";

    @PluginParameter(name = "para", label = "para label")
    private String para;

    @Override
    public ResponseEntity<InputStreamResource> apply(ServicePluginParameters pParameters,
            HttpServletResponse pResponse) {

        LinkedHashSet<DataObject> responseList;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        pResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        if (!para.equals(EXPECTED_VALUE)) {
            responseList = Sets.newLinkedHashSet();
        } else {
            Model model = Model.build("pName", "pDescription", EntityType.DATA);
            DataObject do1 = new DataObject(model, "pTenant", "pLabel1");
            DataObject do2 = new DataObject(model, "pTenant", "pLabel2");
            responseList = Sets.newLinkedHashSet(do1, do2);
        }

        // Format to json format
        GsonBuilder builder = new GsonBuilder();

        InputStreamResource response = new InputStreamResource(
                new ByteArrayInputStream(builder.create().toJson(responseList).getBytes()));

        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

}
