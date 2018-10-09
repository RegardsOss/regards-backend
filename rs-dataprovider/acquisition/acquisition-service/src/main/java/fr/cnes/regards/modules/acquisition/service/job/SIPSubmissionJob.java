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
package fr.cnes.regards.modules.acquisition.service.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
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

    public static final String INGEST_CHAIN_PARAMETER = "chain";

    public static final String SESSION_PARAMETER = "session";

    public static final String DOT = ".";

    public static final String SPACE = " ";

    private String ingestChain;

    private Optional<String> session;

    @Autowired
    private IProductService productService;

    @Autowired
    private IIngestClient ingestClient;

    @Autowired
    private Gson gson;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        ingestChain = getValue(parameters, INGEST_CHAIN_PARAMETER);
        session = getOptionalValue(parameters, SESSION_PARAMETER);
    }

    @Override
    public void run() {
        logger.debug("Processing SIP submission for ingest chain \"{}\" and session \"{}\"", ingestChain, session);
        runByPage();
    }

    /**
     * Make a SIP submission by page
     * @param common SIP submission job report
     */
    private void runByPage() {

        if (Thread.interrupted()) {
            return;
        }

        // Retrieve all products to submit by ingest chain, session page
        // Page size is limited by the property "bulkRequestLimit"
        Page<Product> products = productService.findProductsToSubmit(ingestChain, session);

        if (products.getNumberOfElements() > 0) {

            long startTime = System.currentTimeMillis();
            logger.info("Ingest chain {} - session {} : processing {} products of {}", ingestChain, session,
                        products.getNumberOfElements(), products.getTotalElements());

            // Create SIP collection
            SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder(ingestChain, session.orElse(null));
            products.getContent().forEach(p -> sipCollectionBuilder.add(p.getSip()));

            try {
                // Enable system call as follow (thread safe action)
                FeignSecurityManager.asSystem();
                // Submit SIP collection
                ResponseEntity<Collection<SIPDto>> response = ingestClient.ingest(sipCollectionBuilder.build());
                // Handle response
                handleResponse(response.getStatusCode(), response.getBody(), products.getContent());
            } catch (HttpClientErrorException e) {
                // Handle non 2xx or 404 status code
                Collection<SIPDto> dtos = null;
                if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                    @SuppressWarnings("serial")
                    TypeToken<Collection<SIPDto>> bodyTypeToken = new TypeToken<Collection<SIPDto>>() {
                    };
                    dtos = gson.fromJson(e.getResponseBodyAsString(), bodyTypeToken.getType());
                }
                handleResponse(e.getStatusCode(), dtos, products.getContent());
            } finally {
                // Disable system call if necessary after client request(s)
                FeignSecurityManager.reset();
            }

            logger.info("Ingest chain {} - session {} : {} products of {} processed in {} milliseconds", ingestChain,
                        session, products.getNumberOfElements(), products.getTotalElements(),
                        System.currentTimeMillis() - startTime);
        }

        // Continue if remaining page
        if (products.hasNext()) {
            runByPage();
        }
    }

    /**
     * Handle INGEST response
     * @param status INGEST response status
     * @param response INGEST response
     * @param products list of related products
     */
    private void handleResponse(HttpStatus status, Collection<SIPDto> response, List<Product> products) {
        switch (status) {
            case CREATED:
            case PARTIAL_CONTENT:
            case UNPROCESSABLE_ENTITY:
                // Convert product list to map
                Map<String, Product> productMap = products.stream()
                        .collect(Collectors.toMap(p -> p.getSip().getId(), p -> p));

                // Process all SIP to update all products!
                for (SIPDto dto : response) {
                    Product product = productMap.get(dto.getId());
                    product.setSipState(dto.getState());
                    if (dto.getRejectionCauses() != null && !dto.getRejectionCauses().isEmpty()) {
                        StringBuffer error = new StringBuffer();
                        for (String cause : dto.getRejectionCauses()) {
                            error.append(cause);
                            if (!cause.endsWith(DOT)) {
                                error.append(DOT);
                            }
                            error.append(SPACE);
                        }
                        product.setError(error.toString());
                    }
                    productService.save(product);
                }
                break;
            default:
                String message = String.format("SIP submission failure for ingest chain \"%s\" and session \"%s\"",
                                               ingestChain, session);
                logger.error(message);
                throw new JobRuntimeException(message);
        }
    }
}
