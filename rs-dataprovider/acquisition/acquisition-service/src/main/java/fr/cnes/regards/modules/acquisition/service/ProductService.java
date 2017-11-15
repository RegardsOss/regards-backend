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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductService productService;

    @Autowired
    private IIngestClient ingestClient;

    @Value("${regards.acquisition.sip.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    public ProductService(IProductRepository repository) {
        super();
        this.productRepository = repository;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product retrieve(Long id) {
        return productRepository.findOne(id);
    }

    @Override
    public List<Product> retrieveAll() {
        final List<Product> products = new ArrayList<>();
        productRepository.findAll().forEach(c -> products.add(c));
        return products;
    }

    @Override
    public void delete(Long id) {
        this.productRepository.delete(id);
    }

    @Override
    public Product retrieve(String productName) {
        return productRepository.findCompleteByProductName(productName);
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public List<Product> findBySavedAndStatusIn(Boolean saved, ProductStatus... status) {
        return productRepository.findBySavedAndStatusIn(saved, status);
    }

    @Override
    public void postSIPBulkRequest() {
        LOG.debug("Start bulk request SIP creation");

        //        testParcours();
        //        testParcours2();

        // Get all the ingestChain for that at least one product is ready to be send to ingest
        Set<String> ingestChains = productRepository
                .findDistinctIngestChainBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED);

        if (ingestChains.size() == 0) {
            LOG.info("Any products ready for SIP creation");
            return;
        }

        LOG.info("{} ingest chains found", ingestChains.size());

        for (String ingestChain : ingestChains) {
            postSIPBulkRequest(ingestChain);
        }

        LOG.debug("End  bulk request SIP creation");
    }

    /**
     * Send POST SIP bulk request for an ingest processing chain
     * @param ingestChain an ingest processing chain
     */
    private void postSIPBulkRequest(String ingestChain) {
        LOG.info("Send SIP for ingest chain [{}]", ingestChain);

        // Get all the session
        Set<String> sessions = productRepository
                .findDistinctSessionByIngestChainAndSavedAndStatusIn(ingestChain, false, ProductStatus.COMPLETED,
                                                                     ProductStatus.FINISHED);
        for (String session : sessions) {
            boolean stop = false;
            while (!stop) {
                stop = !postSIPOnePage(ingestChain, session, new PageRequest(0, bulkRequestLimit));
            }
        }
    }

    /**
     * Send one POST SIP request for an ingest processing chain for a sessions id
     * @param ingestChain an ingest processing chain
     * @param session a session
     * @param pageable a {@link Pageable}
     * @return the {@link Page} of {@link Product}
     */
    private boolean postSIPOnePage(String ingestChain, String session, Pageable pageable) {
        Page<Product> page = productRepository
                .findAllByIngestChainAndSessionAndSavedAndStatusIn(ingestChain, session, false, pageable,
                                                                   ProductStatus.COMPLETED, ProductStatus.FINISHED);
        return postSipProducts(ingestChain, session, page.getContent());
    }

    /**
     * Generate a {@link SIPCollection} for this {@link Product}s and send it to the ingest microservice
     * 
     * @param products the {@link List} of {@link Product} ready to send
     */
    private boolean postSipProducts(String ingestChain, String session, List<Product> products) {
        LOG.info("[{}] {} products found", session, products.size());
        SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder(ingestChain, session);
        for (Product product : products) {
            LOG.debug("[{}] product <{}> add to SIP", session, product.getProductName());
            sipCollectionBuilder.add(product.getSip());
        }

        return postSipCollection(session, sipCollectionBuilder.build()) > 0;
    }

    /**
     * Send the {@link SIPCollection} to Ingest microservice
     * @param session
     * @param sipCollection
     * @return the number of {@link Product} that has been saved in Ingest
     */
    private int postSipCollection(String session, SIPCollection sipCollection) {
        LOG.info("[{}] Start publish SIP Collections", session);
        int nbSipOk = 0;

        ResponseEntity<Collection<SIPEntity>> response = ingestClient.ingest(sipCollection);

        if (response.getStatusCode().equals(HttpStatus.CREATED)) {
            nbSipOk = responseSipCreated(session, sipCollection);
        } else if (response.getStatusCode().equals(HttpStatus.PARTIAL_CONTENT)) {
            nbSipOk = responseSipPartiallyCreated(session, response.getBody(), sipCollection);
        } else if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            LOG.error("[{}] Unauthorized access to ingest microservice", session);

        } else if (response.getStatusCode().equals(HttpStatus.CONFLICT)) {
            LOG.error("[{}] Unauthorized access to ingest microservice", session);

        }

        LOG.info("[{}] End publish SIP Collections", session);

        return nbSipOk;
    }

    /**
     * 
     * @param session
     * @param sipCollection
     * @return the number of {@link Product} that has been saved in Ingest
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
     * 
     * @param session
     * @param sipEntitys
     * @param sipCollection
     * @return the number of {@link Product} that has been saved in Ingest
     */
    private int responseSipPartiallyCreated(String session, Collection<SIPEntity> sipEntitys,
            SIPCollection sipCollection) {
        LOG.error("[{}] SIP collection has heen partially processed with success : {} ingested / {} rejected", session,
                  sipCollection.getFeatures().size() - sipEntitys.size(), sipEntitys.size());
        Set<String> sipIdError = new HashSet<>();
        for (SIPEntity sipEntity : sipEntitys) {
            LOG.error("[{}] SIP in error : productName=<{}>, raeson=<{}>", session, sipEntity.getSipId(),
                      sipEntity.getReasonForRejection());
            sipIdError.add(sipEntity.getSipId());
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

    private void setSipProductSaved(String sipId) {
        Product product = productService.retrieve(sipId);
        if (product == null) {
            final StringBuffer strBuff = new StringBuffer();
            strBuff.append("The product name <");
            strBuff.append(sipId);
            strBuff.append("> does not exist");
            LOG.error(strBuff.toString());
        }
        product.setSaved(Boolean.TRUE);
        productService.save(product);
    }

}
