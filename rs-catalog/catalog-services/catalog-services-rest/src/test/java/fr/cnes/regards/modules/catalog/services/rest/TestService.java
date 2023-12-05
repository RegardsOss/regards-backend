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
package fr.cnes.regards.modules.catalog.services.rest;

import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.model.domain.Model;
import org.assertj.core.util.Sets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(id = "tata",
        description = "plugin for test",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss",
        version = "1.0.0")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class TestService implements IService {

    public static final String EXPECTED_VALUE = "skydiving";

    @PluginParameter(name = "para", label = "para label")
    private String para;

    @Override
    public ResponseEntity<StreamingResponseBody> apply(ServicePluginParameters pParameters,
                                                       HttpServletResponse pResponse) {

        LinkedHashSet<DataObject> responseList;

        if (!para.equals(EXPECTED_VALUE)) {
            responseList = Sets.newLinkedHashSet();
        } else {
            Model model = Model.build("pName", "pDescription", EntityType.DATA);
            DataObject do1 = new DataObject(model, "pTenant", "DO1", "pLabel1");
            do1.setIpId(UniformResourceName.build(OAISIdentifier.AIP.name(),
                                                  EntityType.DATA,
                                                  "pTenant",
                                                  UUID.fromString("924d1f0d-37ba-4da1-9be3-d94aac629897"),
                                                  1,
                                                  null,
                                                  null));
            DataObject do2 = new DataObject(model, "pTenant", "DO2", "pLabel2");
            do2.setIpId(new OaisUniformResourceName(OAISIdentifier.AIP,
                                                    EntityType.DATA,
                                                    "pTenant",
                                                    UUID.fromString("74f2c965-0136-47f0-93e1-4fd098db701c"),
                                                    1,
                                                    null,
                                                    null));
            responseList = Sets.newLinkedHashSet(do1, do2);
        }

        return CatalogPluginResponseFactory.createSuccessResponse(pResponse,
                                                                  CatalogPluginResponseType.JSON,
                                                                  responseList);
    }

}
