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
package fr.cnes.regards.modules.ingest.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;

/**
 *
 * Asynchronous client implementation based on the message broker for requesting the ingest service.<br />
 * As this client use message broker to communicate with the storage service, responses are asynchronous. Nevertheless,
 * you can easily listen for responses by implementing your own {@link IIngestClientListener}.
 * @author Marc SORDI
 *
 */
@Component
@RegardsTransactional
public class IngestClient implements IIngestClient {

    @Autowired
    private IPublisher publisher;

    @Autowired(required = false)
    private Validator validator;

    @Override
    public RequestInfo ingest(IngestMetadataDto ingestMetadata, SIP sip) throws IngestClientException {
        RequestInfo requestInfo = RequestInfo.build(sip.getId(), null, null);
        IngestRequestFlowItem item = IngestRequestFlowItem.build(requestInfo.getRequestId(), ingestMetadata, sip);
        tryValidate(item);
        publisher.publish(item);
        return requestInfo;
    }

    /**
     * Validate if a {@link Validator} exists! Depends on Hibernate validator for instance!
     */
    private void tryValidate(IngestRequestFlowItem item) throws IngestClientException {

        if (validator != null) {
            // Validate all flow item
            Errors errors = new MapBindingResult(new HashMap<>(), IngestRequestFlowItem.class.getName());
            validator.validate(item, errors);
            if (errors.hasErrors()) {
                throw new IngestClientException(String.format("Invalid request : %s", getErrors(errors)));
            }
        }
    }

    /**
     * Build a set of error string from {@link Errors}
     */
    private String getErrors(Errors errors) {
        Set<String> errs = new HashSet<>();
        errors.getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errs.add(String.format("%s at %s: rejected value [%s].", fieldError.getDefaultMessage(),
                                       fieldError.getField(),
                                       ObjectUtils.nullSafeToString(fieldError.getRejectedValue())));
            } else {
                errs.add(error.getDefaultMessage());
            }
        });

        StringJoiner joiner = new StringJoiner(", ");
        errs.forEach(err -> joiner.add(err));
        return joiner.toString();
    }

}
