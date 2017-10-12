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
package fr.cnes.regards.modules.ingest.rest;

import java.util.Collection;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.service.IIngestService;

/**
 * This controller manages SIP submission API.
 *
 * @author Marc Sordi
 *
 */
@RestController
@ModuleInfo(name = "SIP management module", description = "SIP submission and management", version = "2.0.0-SNAPSHOT",
        author = "CSSI", legalOwner = "CNES", documentation = "TODO")
@RequestMapping(IngestController.TYPE_MAPPING)
public class IngestController {

    public static final String TYPE_MAPPING = "/sips";

    @Autowired
    private IIngestService ingestService;

    /**
     * Manage SIP bulk request
     *
     * @param sips {@link SIPCollection}
     * @return {@link SIPEntity} collection
     * @throws ModuleException if error occurs!
     */
    @ResourceAccess(description = "SIP collections submission (bulk request)")
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    public ResponseEntity<Collection<SIPEntity>> ingest(@Valid @RequestBody SIPCollection sips) throws ModuleException {
        Collection<SIPEntity> sipEntities = ingestService.ingest(sips);
        return ResponseEntity.status(HttpStatus.CREATED).body(sipEntities);
    }
}
