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
package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.domain.ProcessGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProductBulkRequestService implements IProductBulkRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductBulkRequestService.class);

    //    private final IProductRepository productRepository;

    private final IProductService productService;

    private final IChainGenerationService chainGenerationService;

    private final IIngestClient ingestClient;

    private final IProcessGenerationService processService;

    @Value("${regards.acquisition.sip.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    public ProductBulkRequestService(IProductService productService, IChainGenerationService chainGenerationService,
            IIngestClient ingestClient, IProcessGenerationService processService) {
        this.productService = productService;
        this.chainGenerationService = chainGenerationService;
        this.ingestClient = ingestClient;
        this.processService = processService;
    }

    @Override
    public void runBulkRequest() {
        LOG.debug("Start bulk request SIP creation");

        // Get all the ingestChain for that at least one product is ready to be send to ingest
        Set<String> ingestChains = productService
                .findDistinctIngestChainBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED);

        if (ingestChains.isEmpty()) {
            LOG.debug("Any products ready for SIP creation");
            return;
        }

        LOG.info("{} ingest chains found", ingestChains.size());

        for (String ingestChain : ingestChains) {
            postSIPBulkRequest(ingestChain);
        }

        LOG.debug("End   bulk request SIP creation");
    }

    @Override
    public void runActiveChains() {
        LOG.info("Start run active chains");

        chainGenerationService.findByActiveTrueAndRunningFalse().forEach(ch -> chainGenerationService.run(ch));

        LOG.info("End   run active chains");
    }

    /**
     * Send POST SIP bulk request for an ingest processing chain.
     * @param ingestChain an Ingest processing chain identifier
     */
    private void postSIPBulkRequest(String ingestChain) {
        LOG.info("[{}] Send SIP", ingestChain);

        // Get all the session
        Set<String> sessions = productService
                .findDistinctSessionByIngestChainAndSendedAndStatusIn(ingestChain, false, ProductStatus.COMPLETED,
                                                                      ProductStatus.FINISHED);
        for (String session : sessions) {
            boolean stop = false;
            while (!stop) {
                stop = !postSIPOnePage(ingestChain, session, new PageRequest(0, bulkRequestLimit));
            }
        }
    }

    /**
     * Send one POST SIP request for an ingest processing chain for a sessions id.
     * @param ingestChain an Ingest processing chain identifier
     * @param session a current session identifier
     * @param pageable a {@link Pageable}
     * @return true if at least one {@link Product} has been send to Ingest microservice
     */
    private boolean postSIPOnePage(String ingestChain, String session, Pageable pageable) {
        Page<Product> page = productService
                .findAllByIngestChainAndSessionAndSendedAndStatusIn(ingestChain, session, false, pageable,
                                                                    ProductStatus.COMPLETED, ProductStatus.FINISHED);
        if (page.getContent().isEmpty()) {
            return false;
        }
        return postSipProducts(ingestChain, session, page.getContent());
    }

    /**
     * Generate a {@link SIPCollection} for this {@link Product}s and send it to the Ingest microservice.
     * @param ingestChain an Ingest processing chain identifier
     * @param session a current session identifier
     * @param products the {@link List} of {@link Product} ready to send
     * @return true if at least one {@link Product} has been send to Ingest microservice
     */
    private boolean postSipProducts(String ingestChain, String session, List<Product> products) {
        if (products.isEmpty()) {
            return false;
        }
        LOG.info("[{}] {} products found", session, products.size());
        SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder(ingestChain, session);
        for (Product product : products) {
            LOG.debug("[{}] product <{}> add to SIP", session, product.getProductName());
            sipCollectionBuilder.add(product.getSip());
        }

        return 0 < postSipCollection(session, sipCollectionBuilder.build());
    }

    /**
     * Send the {@link SIPCollection} to Ingest microservice.
     * @param session a current session identifier
     * @param sipCollection the {@link SIPCollection} send to Ingest microservice
     * @return the number of {@link Product} that has been sended to Ingest microservice
     */
    private int postSipCollection(String session, SIPCollection sipCollection) {
        LOG.info("[{}] Start publish SIP Collections", session);
        int nbSipOk = 0;

        ResponseEntity<Collection<SIPDto>> response = ingestClient.ingest(sipCollection);

        if (response.getStatusCode().equals(HttpStatus.CREATED)) {
            nbSipOk = responseSipCreated(session, sipCollection);
            updateProcessGeneration(session, nbSipOk, 0);
        } else if (response.getStatusCode().equals(HttpStatus.PARTIAL_CONTENT)) {
            nbSipOk = responseSipPartiallyCreated(session, response.getBody(), sipCollection);
            updateProcessGeneration(session, nbSipOk, response.getBody().size());
        } else if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            LOG.error("[{}] Unauthorized access to ingest microservice", session);

        } else if (response.getStatusCode().equals(HttpStatus.CONFLICT)) {
            LOG.error("[{}] Unauthorized access to ingest microservice", session);

        }

        LOG.info("[{}] End  publish SIP Collections", session);

        return nbSipOk;
    }

    /**
     * SIP bulk request has been processed with success, change the {@link Product} state.
     * @param session a current session identifier
     * @param sipCollection the {@link SIPCollection} send to Ingest microservice
     * @return the number of {@link Product} that has been sended in Ingest microservice
     */
    private int responseSipCreated(String session, SIPCollection sipCollection) {
        LOG.info("[{}] SIP collection has heen processed with success : {} SIP ingested", session,
                 sipCollection.getFeatures().size());
        for (SIP sip : sipCollection.getFeatures()) {
            setSipProductSaved(sip.getId());
        }
        return sipCollection.getFeatures().size();
    }

    /**
     * SIP bulk request has been processed and some SIP has been rejected, change the {@link Product} state.
     * @param session a current session identifier
     * @param rejectedSips the {@link Collection} of {@link SIPEntity} that have been rejected
     * @param sipCollection the {@link SIPCollection} send to Ingest microservice
     * @return the number of {@link Product} that has been sended to Ingest microservice
     */
    private int responseSipPartiallyCreated(String session, Collection<SIPDto> rejectedSips,
            SIPCollection sipCollection) {
        LOG.info("features size:{} - rejected size:{}", sipCollection.getFeatures().size(), rejectedSips.size());
        LOG.error("[{}] SIP collection has heen partially processed with success : {} ingested / {} rejected", session,
                  sipCollection.getFeatures().size() - rejectedSips.size(), rejectedSips.size());
        Set<String> sipIdError = new HashSet<>();
        for (SIPDto sipEntity : rejectedSips) {
            LOG.error("[{}] SIP in error : productName=<{}>, reason=<{}>", session, sipEntity.getIpId(),
                      sipEntity.getRejectionCauses());
            sipIdError.add(sipEntity.getIpId());
            
            Product pp = productService.retrieve(sipEntity.getIpId());
            pp.setStatus(ProductStatus.ERROR);
            productService.save(pp);
        }

        int nbSipOK = 0;
        for (SIP sip : sipCollection.getFeatures()) {
            if (!sipIdError.contains(sip.getId())) {
                setSipProductSaved(sip.getId());
                nbSipOK++;
            }
        }
        return nbSipOK;
    }

    /**
     * Change the state of a {@link Product} and persists it.
     * @param sipId the {@link Product} identifier
     */
    private void setSipProductSaved(String sipId) {
        Product product = productService.retrieve(sipId);
        if (product == null) {
            final StringBuilder buff = new StringBuilder();
            buff.append("The product name <");
            buff.append(sipId);
            buff.append("> does not exist");
            LOG.error(buff.toString());
        } else {
            product.setSended(Boolean.TRUE);
            productService.save(product);
        }
    }

    /**
     * Update the {@link ProcessGeneration}
     * @param session a current session identifier
     * @param nbSipCreated the number of SIP that are in {@link SIPState#CREATED}
     * @param nbSipError the number of SIP that are in {@link SIPState#REJECTED}
     */
    private void updateProcessGeneration(String session, int nbSipCreated, int nbSipError) {
        ProcessGeneration processGeneration = processService.findBySession(session);
        if (processGeneration != null) {
            processGeneration.setNbSipCreated(processGeneration.getNbSipCreated() + nbSipCreated);
            processGeneration.setNbSipError(processGeneration.getNbSipError() + nbSipError);
            if (processGeneration.getNbSipCreated() == processGeneration.getNbSipStored()
                    + processGeneration.getNbSipError()) {
                processGeneration.setStopDate(OffsetDateTime.now());
            }
            processService.save(processGeneration);
        }

    }
}
