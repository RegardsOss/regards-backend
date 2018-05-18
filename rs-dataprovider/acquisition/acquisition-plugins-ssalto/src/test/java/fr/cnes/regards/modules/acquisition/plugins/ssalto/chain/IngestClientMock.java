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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Simulate ingest service
 *
 * @author Marc Sordi
 *
 */
public class IngestClientMock implements IIngestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestClientMock.class);

    @Autowired
    private Validator validator;

    @Override
    public ResponseEntity<Collection<SIPDto>> ingest(SIPCollection sips) {

        Collection<SIPDto> result = new ArrayList<>();

        // Just validate SIP and return DTO
        for (SIP sip : sips.getFeatures()) {
            Errors errors = new MapBindingResult(new HashMap<>(), sip.getId());
            validator.validate(sip, errors);
            result.add(buildDto(sip, errors));
        }

        HttpStatus status = computeStatus(result);
        return ResponseEntity.status(status).body(result);
    }

    private SIPDto buildDto(SIP sip, Errors errors) {
        SIPDto dto = new SIPDto();

        if (errors.hasErrors()) {
            dto.setState(SIPState.INVALID);

            List<String> rejectionCauses = new ArrayList<>();
            errors.getAllErrors().forEach(error -> rejectionCauses.add(error.toString()));
            dto.setRejectionCauses(rejectionCauses);
        } else {
            dto.setState(SIPState.VALID);
            dto.setId(sip.getId());
            dto.setIpId("unique_" + sip.getId());
        }

        return dto;
    }

    // Copy from SIPController
    private HttpStatus computeStatus(Collection<SIPDto> dtos) {
        Boolean hasCreated = Boolean.FALSE;
        Boolean hasRejected = Boolean.FALSE;
        for (SIPDto sipEntity : dtos) {
            switch (sipEntity.getState()) {
                case CREATED:
                    hasCreated = Boolean.TRUE;
                    break;
                case REJECTED:
                    hasRejected = Boolean.TRUE;
                    break;
                default:
                    LOGGER.warn("Unexpected SIP state");
                    break;
            }
        }
        HttpStatus status;
        if (hasCreated && hasRejected) {
            status = HttpStatus.PARTIAL_CONTENT; // 206
        } else if (hasRejected) {
            status = HttpStatus.UNPROCESSABLE_ENTITY; // 422
        } else {
            status = HttpStatus.CREATED; // 201
        }
        return status;
    }

}
