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
package fr.cnes.regards.modules.acquisition.service.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;

/**
 * This job manages SIP submission (i.e. INGEST bulk request) for a specified session and chain.
 *
 * @author Marc Sordi
 *
 */
public class SIPSubmissionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPSubmissionJob.class);

    public static final String INGEST_CHAIN_PARAMETER = "chain";

    public static final String SESSION_PARAMETER = "session";

    private String ingestChain;

    private String session;

    @Autowired
    private IProductService productService;

    @Autowired
    private IIngestClient ingestClient;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        ingestChain = getValue(parameters, INGEST_CHAIN_PARAMETER);
        session = getValue(parameters, SESSION_PARAMETER);
    }

    @Override
    public void run() {
        LOGGER.debug("Processing SIP submission for ingest chain \"{}\" and session \"{}\"", ingestChain, session);
        runByPage();
    }

    /**
     * Make a SIP submission by page
     */
    private void runByPage() {

        // Retrieve all products to submit by ingest chain, session page
        // Page size is limited by the property "bulkRequestLimit"
        Page<Product> products = productService.findProductsToSubmit(ingestChain, session);

        if (products.getNumberOfElements() > 0) {
            LOGGER.debug("Ingest chain {} - session {} : processing {} products of {}", ingestChain, session,
                         products.getNumberOfElements(), products.getTotalElements());
            // Create SIP collection
            SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder(ingestChain, session);
            products.getContent().forEach(p -> sipCollectionBuilder.add(p.getSip()));
            // Submit SIP collection
            ResponseEntity<Collection<SIPDto>> response = ingestClient.ingest(sipCollectionBuilder.build());
            // Handle response
            handleResponse(response, products.getContent());
        }

        // Continue if remaining page
        if (products.hasNext()) {
            runByPage();
        }
    }

    /**
     * Handle INGEST response
     * @param response INGEST response
     * @param products list of related products
     */
    private void handleResponse(ResponseEntity<Collection<SIPDto>> response, List<Product> products) {
        switch (response.getStatusCode()) {
            case CREATED:
            case PARTIAL_CONTENT:
            case UNPROCESSABLE_ENTITY:
                // Convert product list to map
                Map<String, Product> productMap = products.stream()
                        .collect(Collectors.toMap(p -> p.getSip().getId(), p -> p));
                // Process all SIP to update all products!
                for (SIPDto dto : response.getBody()) {
                    Product product = productMap.get(dto.getId());
                    product.setSipState(dto.getState());
                    product.setIpId(dto.getIpId()); // May be null
                    productService.save(product);
                }
                break;
            default:
                String message = String.format("SIP submission failure for ingest chain \"%s\" and session \"%s\"",
                                               ingestChain, session);
                LOGGER.error(message);
                throw new JobRuntimeException(message);
        }
    }
}
