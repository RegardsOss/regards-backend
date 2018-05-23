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
package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.ProductSpecifications;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.PostAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPSubmissionJob;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * Manage acquisition {@link Product}
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Value("${regards.acquisition.sip.bulk.request.limit:1000}")
    private Integer bulkRequestLimit;

    @Override
    public Product save(Product product) {
        product.setLastUpdate(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @Override
    public Product loadProduct(Long id) throws ModuleException {
        Product product = productRepository.findCompleteById(id);
        if (product == null) {
            throw new EntityNotFoundException(id, Product.class);
        }
        return product;
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
    public Optional<Product> searchProduct(String productName) throws ModuleException {
        return Optional.ofNullable(productRepository.findCompleteByProductName(productName));
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

    @Override
    public Set<Product> findChainProducts(AcquisitionProcessingChain chain) {
        return productRepository.findByProcessingChain(chain);
    }

    /**
     * Schedule a {@link SIPGenerationJob} and update product SIP state in same transaction.
     */
    @Override
    public JobInfo scheduleProductSIPGeneration(Product product, AcquisitionProcessingChain chain) {

        // Schedule job
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.SIP_GENERATION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, chain.getId()),
                              new JobParameter(SIPGenerationJob.PRODUCT_ID, product.getId()));
        jobInfo.setClassName(SIPGenerationJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());
        jobInfo = jobInfoService.createAsQueued(jobInfo);

        // Release lock
        if (product.getLastSIPGenerationJobInfo() != null) {
            jobInfoService.unlock(product.getLastSIPGenerationJobInfo());
        }

        // Change product SIP state
        product.setSipState(ProductSIPState.SCHEDULED);
        product.setLastSIPGenerationJobInfo(jobInfo);
        productRepository.save(product);

        return jobInfo;
    }

    @Override
    public Set<Product> findByStatus(ProductState status) {
        return productRepository.findByState(status);
    }

    private void computeProductState(Product product) {
        // At least one mandatory file is VALID
        product.setState(ProductState.ACQUIRING);

        int nbTotalMandatory = 0;
        int nbTotalOptional = 0;
        int nbActualMandatory = 0;
        int nbActualOptional = 0;

        // Compute product requirements
        for (AcquisitionFileInfo fileInfo : product.getProcessingChain().getFileInfos()) {
            if (fileInfo.isMandatory()) {
                nbTotalMandatory++;
            } else {
                nbTotalOptional++;
            }
        }

        // Compute product state
        for (AcquisitionFile file : product.getAcquisitionFiles()) {
            if (AcquisitionFileState.VALID.equals(file.getState())
                    || AcquisitionFileState.ACQUIRED.equals(file.getState())) {
                if (file.getFileInfo().isMandatory()) {
                    nbActualMandatory++;
                } else {
                    nbActualOptional++;
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
        // Get the product if exists
        Product currentProduct = productRepository.findCompleteByProductName(productName);

        if (currentProduct == null) {
            // It is a new Product, create it
            currentProduct = new Product();
            currentProduct.setProductName(productName);
            currentProduct.setProcessingChain(processingChain);
        } else {
            // Mark old file as superseded
            for (AcquisitionFile existing : currentProduct.getAcquisitionFiles()) {
                if (existing.getFileInfo().equals(acqFile.getFileInfo())) {
                    if (AcquisitionFileState.ACQUIRED.equals(existing.getState())) {
                        existing.setState(AcquisitionFileState.SUPERSEDED);
                        acqFileRepository.save(existing);
                    }
                    if (AcquisitionFileState.ERROR.equals(existing.getState())) {
                        existing.setState(AcquisitionFileState.SUPERSEDED_AFTER_ERROR);
                        acqFileRepository.save(existing);
                    }
                }
            }
        }

        currentProduct.setSipState(ProductSIPState.NOT_SCHEDULED); // Required to be re-integrated in SIP workflow
        currentProduct.setSession(session);
        currentProduct.addAcquisitionFile(acqFile);
        computeProductState(currentProduct);

        // Mark file as acquired
        acqFile.setState(AcquisitionFileState.ACQUIRED);
        acqFileRepository.save(acqFile);

        return save(currentProduct);
    }

    @Override
    public Page<Product> findProductsToSubmit(String ingestChain, Optional<String> session) {

        if (session.isPresent()) {
            return productRepository.findByProcessingChainIngestChainAndSessionAndSipState(ingestChain, session
                    .get(), ProductSIPState.SUBMISSION_SCHEDULED, new PageRequest(0, bulkRequestLimit));
        } else {
            return productRepository.findByProcessingChainIngestChainAndSipState(ingestChain,
                                                                                 ProductSIPState.SUBMISSION_SCHEDULED,
                                                                                 new PageRequest(0, bulkRequestLimit));
        }
    }

    /**
     * This method is called by a time scheduler. We only schedule a new job for a specified chain and session if and
     * only if an existing job not already exists. To detect that a job is already scheduled, we check the SIP state of
     * the products. Product not already scheduled will be scheduled on next scheduler call.
     */
    @Override
    public void scheduleProductSIPSubmission() {
        // Find all products already scheduled for submission
        Set<Product> products = productRepository.findWithLockBySipState(ProductSIPState.SUBMISSION_SCHEDULED);

        // Register all chains and sessions already scheduled
        Multimap<String, String> scheduledSessionsByChain = ArrayListMultimap.create();
        if ((products != null) && !products.isEmpty()) {
            for (Product product : products) {
                if (!scheduledSessionsByChain.containsEntry(product.getProcessingChain().getIngestChain(),
                                                            product.getSession())) {
                    scheduledSessionsByChain.put(product.getProcessingChain().getIngestChain(), product.getSession());
                }
            }
        }

        // Find all products with available SIPs ready for submission
        products = productRepository.findWithLockBySipState(ProductSIPState.GENERATED);

        if ((products != null) && !products.isEmpty()) {

            // Register products per ingest chain and session for reporting
            Map<String, Map<String, List<Product>>> productsPerIngestChain = new HashMap<>();

            Multimap<String, String> sessionsByChain = ArrayListMultimap.create();
            for (Product product : products) {
                // Check if chain and session not already scheduled
                if (!scheduledSessionsByChain.containsEntry(product.getProcessingChain().getIngestChain(),
                                                            product.getSession())) {
                    // Register chains and sessions for scheduling
                    if (!sessionsByChain.containsEntry(product.getProcessingChain().getIngestChain(),
                                                       product.getSession())) {
                        sessionsByChain.put(product.getProcessingChain().getIngestChain(), product.getSession());
                    }

                    // Register product
                    registerProduct(productsPerIngestChain, product);

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
                    jobParameters.add(new JobParameter(SIPSubmissionJob.INGEST_CHAIN_PARAMETER, ingestChain));
                    jobParameters.add(new JobParameter(SIPSubmissionJob.SESSION_PARAMETER, session));

                    JobInfo jobInfo = new JobInfo(true);
                    jobInfo.setPriority(AcquisitionJobPriority.SIP_SUBMISSION_JOB_PRIORITY.getPriority());
                    jobInfo.setParameters(jobParameters);
                    jobInfo.setClassName(SIPSubmissionJob.class.getName());
                    jobInfo.setOwner(authResolver.getUser());
                    jobInfoService.createAsQueued(jobInfo);

                    // Link report to all related products
                    linkSubmissionJobInfo(productsPerIngestChain, jobInfo, ingestChain, session);
                }
            }
        }
    }

    /**
     * Register product
     * @param productPerIngestChain list of product per session per ingest chain
     * @param product {@link Product} to register
     */
    private void registerProduct(Map<String, Map<String, List<Product>>> productsPerIngestChain, Product product) {
        String ingestChain = product.getProcessingChain().getIngestChain();
        String session = product.getSession();

        Map<String, List<Product>> productsPerSession = productsPerIngestChain.get(ingestChain);
        if (productsPerSession == null) {
            productsPerSession = new HashMap<>();
            productsPerIngestChain.put(ingestChain, productsPerSession);
        }

        List<Product> products = productsPerSession.get(session);
        if (products == null) {
            products = new ArrayList<>();
            productsPerSession.put(session, products);
        }

        products.add(product);
    }

    /**
     * Link submission report to related products
     * @param productsPerIngestChain list of registered products
     * @param jobInfo {@link JobInfo} to link
     * @param ingestChain INGEST chain
     * @param session INGEST session
     */
    private void linkSubmissionJobInfo(Map<String, Map<String, List<Product>>> productsPerIngestChain, JobInfo jobInfo,
            String ingestChain, String session) {

        Map<String, List<Product>> productsPerSession = productsPerIngestChain.get(ingestChain);
        if (productsPerSession != null) {
            List<Product> products = productsPerSession.get(session);
            if (products != null) {
                for (Product product : products) {
                    // Release lock
                    if (product.getLastSIPSubmissionJobInfo() != null) {
                        jobInfoService.unlock(product.getLastSIPSubmissionJobInfo());
                    }
                    product.setLastSIPSubmissionJobInfo(jobInfo);
                    save(product);
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
    public void handleProductJobEvent(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())) {
            // Load job info
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            handleSIPSubmissiontError(jobInfo);
            try {
                handleSIPGenerationError(jobInfo);
            } catch (ModuleException e) {
                LOGGER.error("Error handling SIP generation error", e);
            }
        }
    }

    private void handleSIPSubmissiontError(JobInfo jobInfo) {
        if (SIPSubmissionJob.class.getName().equals(jobInfo.getClassName())) {
            Map<String, JobParameter> params = jobInfo.getParametersAsMap();
            String ingestChain = params.get(SIPSubmissionJob.INGEST_CHAIN_PARAMETER).getValue();
            String session = params.get(SIPSubmissionJob.SESSION_PARAMETER).getValue();
            // Update product status
            Set<Product> products;
            if (session == null) {
                products = productRepository
                        .findByProcessingChainIngestChainAndSipState(ingestChain, ProductSIPState.SUBMISSION_SCHEDULED);
            } else {
                products = productRepository
                        .findByProcessingChainIngestChainAndSessionAndSipState(ingestChain, session,
                                                                               ProductSIPState.SUBMISSION_SCHEDULED);
            }
            for (Product product : products) {
                product.setSipState(ProductSIPState.SUBMISSION_ERROR);
                save(product);
            }
        }
    }

    private void handleSIPGenerationError(JobInfo jobInfo) throws ModuleException {
        if (SIPGenerationJob.class.getName().equals(jobInfo.getClassName())) {
            JobParameter productIdParam = jobInfo.getParametersAsMap().get(SIPGenerationJob.PRODUCT_ID);
            if (productIdParam != null) {
                Long productId = productIdParam.getValue();
                Product product = loadProduct(productId);
                product.setSipState(ProductSIPState.GENERATION_ERROR);
                product.setError(jobInfo.getStatus().getStackTrace());
                save(product);
            }
        }
    }

    @Override
    public void retryProductSIPSubmission() {
        Set<Product> products = productRepository.findWithLockBySipState(ProductSIPState.SUBMISSION_ERROR);
        for (Product product : products) {
            product.setSipState(ProductSIPState.GENERATED);
            save(product);
        }
    }

    @Override
    public void handleSIPEvent(SIPEvent event) {
        Product product = productRepository.findCompleteByIpId(event.getIpId());
        if (product != null) {
            // Do post processing if SIP properly stored
            if (SIPState.STORED.equals(event.getState())) {
                JobInfo jobInfo = new JobInfo(true);
                jobInfo.setPriority(AcquisitionJobPriority.POST_ACQUISITION_JOB_PRIORITY.getPriority());
                jobInfo.setParameters(new JobParameter(PostAcquisitionJob.EVENT_PARAMETER, event));
                jobInfo.setClassName(PostAcquisitionJob.class.getName());
                jobInfo.setOwner(authResolver.getUser());
                jobInfo = jobInfoService.createAsQueued(jobInfo);

                // Release lock
                if (product.getLastPostProductionJobInfo() != null) {
                    jobInfoService.unlock(product.getLastPostProductionJobInfo());
                }
                product.setLastPostProductionJobInfo(jobInfo);
            }

            product.setSipState(event.getState());
            productRepository.save(product);
        } else {
            LOGGER.debug("SIP with IP ID \"{}\" is not managed by data provider", event.getIpId());
        }
    }

    @Override
    public long countByChainAndStateIn(AcquisitionProcessingChain processingChain, List<ProductState> productStates) {
        return productRepository.countByProcessingChainAndStateIn(processingChain, productStates);
    }

    @Override
    public long countByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            List<ISipState> productSipStates) {
        return productRepository.countByProcessingChainAndSipStateIn(processingChain, productSipStates);
    }

    @Override
    public long countByChain(AcquisitionProcessingChain chain) {
        return productRepository.countByProcessingChain(chain);
    }

    @Override
    public Page<Product> search(List<ProductState> state, List<ISipState> sipState, String productName, String session,
            Long processingChainId, OffsetDateTime from, Boolean noSession, Pageable pageable) {
        return productRepository.findAll(ProductSpecifications.search(state, sipState, productName, session,
                                                                      processingChainId, from, noSession),
                                         pageable);
    }

    @Override
    public void stopProductJobs(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Handle SIP generation jobs
        Set<Product> products = productRepository.findWithLockByProcessingChainAndSipState(processingChain,
                                                                                           ProductSIPState.SCHEDULED);
        for (Product product : products) {
            jobInfoService.stopJob(product.getLastSIPGenerationJobInfo().getId());
        }

        // Handle SIP submission jobs
        products = productRepository.findWithLockByProcessingChainAndSipState(processingChain,
                                                                              ProductSIPState.SUBMISSION_SCHEDULED);
        Set<JobInfo> submissionJobInfos = new HashSet<>();
        for (Product product : products) {
            submissionJobInfos.add(product.getLastSIPSubmissionJobInfo());
        }
        for (JobInfo jobInfo : submissionJobInfos) {
            jobInfoService.stopJob(jobInfo.getId());
        }
    }

    @Override
    public boolean isProductJobStoppedAndCleaned(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Handle SIP generation jobs
        Set<Product> products = productRepository.findWithLockByProcessingChainAndSipState(processingChain,
                                                                                           ProductSIPState.SCHEDULED);
        for (Product product : products) {
            if (!product.getLastSIPGenerationJobInfo().getStatus().getStatus().isFinished()) {
                return false;
            } else {
                // Clean product state
                product.setSipState(ProductSIPState.NOT_SCHEDULED);
                productRepository.save(product);
            }
        }

        // Handle SIP submission jobs
        products = productRepository.findWithLockByProcessingChainAndSipState(processingChain,
                                                                              ProductSIPState.SUBMISSION_SCHEDULED);
        for (Product product : products) {
            if (!product.getLastSIPSubmissionJobInfo().getStatus().getStatus().isFinished()) {
                return false;
            } else {
                // Clean product state
                product.setSipState(ProductSIPState.GENERATED);
                productRepository.save(product);
            }
        }

        return true;
    }
}
