/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(StorageDataFileController.TYPE_MAPPING)
public class StorageDataFileController implements IResourceController<StorageDataFile> {

    public static final String TYPE_MAPPING = "/data-file";

    public static final String AIP_PATH = "/aip/{id}";

    @Autowired
    private IAIPService aipService;

    /**
     * Service handling hypermedia resources
     */
    @SuppressWarnings("unused")
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the aip files metadata associated to an aip, represented by its ip id
     * @param pIpId
     * @return aip files metadata associated to the aip
     * @throws ModuleException
     */
    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files data files of a specified aip")
    public ResponseEntity<List<Resource<StorageDataFile>>> retrieveAIPFiles(@PathVariable("id") @Valid String pIpId)
            throws ModuleException {
        Set<StorageDataFile> files = aipService.retrieveAIPDataFiles(UniformResourceName.fromString(pIpId));
        return new ResponseEntity<>(toResources(files), HttpStatus.OK);
    }

    @Override
    public Resource<StorageDataFile> toResource(StorageDataFile element, Object... extras) {
        return new Resource<>(element);
    }

    @Override
    public List<Resource<StorageDataFile>> toResources(Collection<StorageDataFile> elements, Object... extras) {
        // Adapt the result to match front expectations
        List<Resource<StorageDataFile>> result = new ArrayList<>(elements.size());
        elements.forEach(f -> result.add(new Resource<>(f)));
        return result;
    }
}
