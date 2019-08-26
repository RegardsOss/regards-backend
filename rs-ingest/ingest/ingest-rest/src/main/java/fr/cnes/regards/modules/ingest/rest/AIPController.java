/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;

/**
 * REST API for managing AIP
 *
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping(SIPController.TYPE_MAPPING)
public class AIPController implements IResourceController<AIPEntity> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    public static final String TYPE_MAPPING = "/aips";

    public static final String AIP_ID_PATH_PARAM = "aip_id";

    public static final String AIP_DOWNLOAD_PATH = "/{" + AIP_ID_PATH_PARAM + "}/download";

    @Autowired
    private IAIPService aipService;

    @RequestMapping(value = AIP_DOWNLOAD_PATH, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "Download AIP as JSON file")
    public void downloadAIP(@RequestParam(required = false) String origin,
            @Valid @PathVariable(AIP_ID_PATH_PARAM) UniformResourceName aipId, HttpServletResponse response)
            throws ModuleException, IOException {

        LOGGER.debug("Downloading AIP file for entity \"{}\"", aipId.toString());

        // FIXME : Get data object from AIP
        ContentInformation ci = null;
        OAISDataObject dataObject = ci.getDataObject();

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + dataObject.getFilename());
        // NOTE : Do not set content type after download. It can be ignored.
        response.setContentType(ci.getRepresentationInformation().getSyntax().getMimeType().toString());
        if (dataObject.getFileSize() != null) {
            response.setContentLengthLong(dataObject.getFileSize());
        }
        try {
            aipService.downloadAIP(aipId, response.getOutputStream());
        } catch (ModuleException e) {
            // Workaround to handle conversion of ServletErrorResponse in JSON format and
            // avoid using ContentType of file set before.
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            throw e;
        }
        response.getOutputStream().flush();
        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    public Resource<AIPEntity> toResource(AIPEntity element, Object... extras) {
        // TODO Auto-generated method stub
        return null;
    }

}
