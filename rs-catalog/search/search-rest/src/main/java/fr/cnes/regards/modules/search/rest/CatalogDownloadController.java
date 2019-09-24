/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.storagelight.client.IStorageRestClient;

/**
 * REST Controller handling operations on downloads.
 *
 * @author Kevin Marchois
 */
@RestController
@RequestMapping(CatalogDownloadController.PATH_DOWNLOAD)
public class CatalogDownloadController {

	public static final String PATH_DOWNLOAD = "/downloads";
	
    public static final String DOWNLOAD_AIP_FILE = "/{aip_id}/files/{checksum}";

    /**
     * AIP ID path parameter
     */
    public static final String AIP_ID_PATH_PARAM = "aip_id";
    

    /**
     * checksum path parameter
     */
    public static final String CHECKSUM_PATH_PARAM = "checksum";

    
    @Autowired
    private IStorageRestClient storageRestClient;
    
    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    /**
     * Download a file that user has right to
     * @param aipId aip id where is the file
     * @param checksum checksum on the file
     * @return the file to download
     * @throws ModuleException
     * @throws IOException 
     */
    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable(AIP_ID_PATH_PARAM) String aipId,
            @PathVariable(CHECKSUM_PATH_PARAM) String checksum) throws ModuleException, IOException {
        UniformResourceName urn = UniformResourceName.fromString(aipId);
    	if (this.searchService.hasAccess(urn)) {
    		return this.storageRestClient.downloadFile(checksum);
    	}
    	return new ResponseEntity<InputStreamResource>(HttpStatus.FORBIDDEN);
        
    }
}
