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
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPSubmissionJob;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;

/**
 * Manage acquisition {@link Product}
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private final IProductRepository productRepository;

    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    @Value("${regards.acquisition.sip.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    public ProductService(IProductRepository repository, IAuthenticationResolver authResolver,
            IJobInfoService jobInfoService,
            ExecAcquisitionProcessingChainService execAcquisitionProcessingChainService) {
        this.productRepository = repository;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
    }

    @Override
    public Product save(Product product) {
        product.setLastUpdate(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @Override
    public Product retrieve(Long id) {
        return productRepository.findOne(id);
    }

    @Override
    public Product retrieve(String productName) throws ModuleException {
        Product product = productRepository.findCompleteByProductName(productName);
        if (product == null) {
            String message = String.format("Product with name \"%s\" not found", productName);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }
        return product;
    }

    @Override
    public Page<Product> retrieveAll(Pageable page) {
        return productRepository.findAll(page);
    }

    @Override
    public void delete(Long id) {
        productRepository.delete(id);
    }

    @Override
    public void delete(Product product) {
        productRepository.delete(product);
    }

    @Override
    public Set<Product> findChainProductsToSchedule(AcquisitionProcessingChain chain) {
        return productRepository.findChainProductsToSchedule(chain);
    }

    /**
     * Schedule a {@link SIPGenerationJob} and update product SIP state in same transaction.
     */
    @Override
    public JobInfo scheduleProductSIPGeneration(Product product, AcquisitionProcessingChain chain) {

        // Schedule job
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new JobParameter(SIPGenerationJob.CHAIN_PARAMETER, chain),
                                  new JobParameter(SIPGenerationJob.PRODUCT_NAME, product.getProductName()));
        acquisition.setClassName(SIPGenerationJob.class.getName());
        acquisition.setOwner(authResolver.getUser());
        acquisition = jobInfoService.createAsQueued(acquisition);

        // Change product SIP state
        product.setSipState(ProductSIPState.SCHEDULED);
        productRepository.save(product);

        return acquisition;
    }

    @Override
    public Set<Product> findByStatus(ProductState status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public void computeProductStatus(Product product) {
        // At least one mandatory file is VALID
        product.setState(ProductState.ACQUIRING);

        if (product.getMetaProduct() == null) {
            LOGGER.error("[{}] The meta product of the product {} <{}> should not be null", product.getSession(),
                         product.getId(), product.getProductName());
            return;
        }

        int nbTotalMandatory = 0;
        int nbTotalOptional = 0;
        int nbActualMandatory = 0;
        int nbActualOptional = 0;

        for (MetaFile mf : product.getMetaProduct().getMetaFiles()) {
            // Calculus the number of mandatory files
            if (mf.isMandatory()) {
                nbTotalMandatory++;
            } else {
                nbTotalOptional++;
            }
            for (AcquisitionFile af : product.getAcquisitionFile()) {
                if (af.getMetaFile().equals(mf) && af.getStatus().equals(AcquisitionFileState.VALID)) {
                    if (mf.isMandatory()) {
                        // At least one mandatory file is VALID
                        nbActualMandatory++;
                    } else {
                        nbActualOptional++;
                    }
                }
            }
        }

        if (nbTotalMandatory == nbActualMandatory) {
            // ProductStatus is COMPLETED if mandatory files is acquired
            product.setState(ProductState.COMPLETED);
            if (nbTotalOptional == nbActualOptional) {
                // ProductStatus is FINISHED if mandatory and optional files is acquired
                product.setState(ProductState.FINISHED);
            }
        }
    }

    @Override
    public Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            AcquisitionProcessingChain processingChain) throws ModuleException {
        // Get the product if it exists
        Product currentProduct = productRepository.findCompleteByProductName(productName);

        if (currentProduct == null) {
            // It is a new Product, create it
            currentProduct = new Product();
            currentProduct.setProductName(productName);
            currentProduct.setProcessingChain(processingChain);
            currentProduct.setSipState(ProductSIPState.NOT_SCHEDULED);
        }

        currentProduct.setSession(session);
        currentProduct.addAcquisitionFile(acqFile);
        computeProductStatus(currentProduct);

        return save(currentProduct);
    }

    @Override
    public Page<Product> findProductsToSubmit(String ingestChain, String session) {
        return productRepository.findByIngestChainAndSessionAndSipState(ingestChain, session,
                                                                        ProductSIPState.SUBMISSION_SCHEDULED,
                                                                        new PageRequest(0, bulkRequestLimit));
    }

    /**
     * This method is called by a time scheduler. We only schedule a new job for a specified chain and session if and
     * only if an existing job not already exists. To detect that a job is already scheduled, we check the SIP state of
     * the products. Product not already scheduled will be scheduled on next scheduler call.
     */
    @Override
    public void scheduleProductSIPSubmission() {
        // Find all products already scheduled for submission
        Set<Product> products = productRepository.findBySipState(ProductSIPState.SUBMISSION_SCHEDULED);

        // Register all chains and sessions already scheduled
        Multimap<String, String> scheduledSessionsByChain = ArrayListMultimap.create();
        if ((products != null) && !products.isEmpty()) {
            for (Product product : products) {
                scheduledSessionsByChain.put(product.getProcessingChain().getIngestChain(), product.getSession());
            }
        }

        // Find all products with available SIPs ready for submission
        products = productRepository.findBySipState(ProductSIPState.GENERATED);

        if ((products != null) && !products.isEmpty()) {

            Multimap<String, String> sessionsByChain = ArrayListMultimap.create();
            for (Product product : products) {
                // Check if chain and session not already scheduled
                if (!scheduledSessionsByChain.containsEntry(product.getProcessingChain().getIngestChain(),
                                                            product.getSession())) {
                    // Register chains and sessions for scheduling
                    sessionsByChain.put(product.getProcessingChain().getIngestChain(), product.getSession());
                    // Update SIP state
                    product.setSipState(ProductSIPState.SUBMISSION_SCHEDULED);
                    save(product);
                }
            }

            // Schedule submission jobs
            for (String ingestChain : sessionsByChain.keySet()) {
                for (String session : sessionsByChain.get(ingestChain)) {
                    // Schedule job
                    Set<JobParameter> jobParameters = Sets.newHashSet();
                    jobParameters.add(new JobParameter(SIPSubmissionJob.CHAIN_PARAMETER, ingestChain));
                    jobParameters.add(new JobParameter(SIPSubmissionJob.SESSION_PARAMETER, session));
                    JobInfo jobInfo = new JobInfo(1, jobParameters, authResolver.getUser(),
                            SIPSubmissionJob.class.getName());
                    jobInfoService.createAsQueued(jobInfo);
                }
            }
        }
    }

    /**
     * If {@link SIPSubmissionJob} fails, no job can be run as long as there are products in
     * {@link ProductSIPState#SUBMISSION_SCHEDULED}. This handler updates product states to
     * {@link ProductSIPState#SUBMISSION_ERROR}.<br>
     */
    @Override
    public void handleProductSIPSubmissionFailure(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())
                && SIPSubmissionJob.class.isAssignableFrom(jobEvent.getClass())) {
            // Load job info
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            Map<String, JobParameter> params = jobInfo.getParametersAsMap();
            String ingestChain = params.get(SIPSubmissionJob.CHAIN_PARAMETER).getValue();
            String session = params.get(SIPSubmissionJob.SESSION_PARAMETER).getValue();
            // Update product status
            Set<Product> products = productRepository
                    .findByIngestChainAndSessionAndSipState(ingestChain, session, ProductSIPState.SUBMISSION_SCHEDULED);
            for (Product product : products) {
                product.setSipState(ProductSIPState.SUBMISSION_ERROR);
                save(product);
            }
        }
    }

    @Override
    public void retryProductSIPSubmission() {
        Set<Product> products = productRepository.findBySipState(ProductSIPState.SUBMISSION_ERROR);
        for (Product product : products) {
            product.setSipState(ProductSIPState.GENERATED);
            save(product);
        }
    }

    @Override
    public void updateProductSIPState(String ipId, ISipState sipState) {
        Product product = productRepository.findCompleteByIpId(ipId);
        product.setSipState(sipState);
    }
}
