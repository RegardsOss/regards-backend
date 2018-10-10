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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
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
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
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
    private IPluginService pluginService;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Value("${regards.acquisition.sip.bulk.request.limit:100}")
    private Integer bulkRequestLimit;

    @Value("${regards.acquisition.pagination.default.page.size:100}")
    private Integer defaultPageSize;

    @Override
    public Product save(Product product) {
        LOGGER.debug("Saving product \"{}\" with IP ID \"{}\" and SIP state \"{}\"", product.getProductName(),
                     product.getIpId(), product.getSipState());
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
    public Set<Product> retrieve(Collection<String> productNames) throws ModuleException {
        return productRepository.findCompleteByProductNameIn(productNames);
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
    public Page<Product> findChainProducts(AcquisitionProcessingChain chain, Pageable pageable) {
        return productRepository.findByProcessingChainOrderByIdAsc(chain, pageable);
    }

    @Override
    public JobInfo scheduleProductSIPGenerations(Set<Product> products, AcquisitionProcessingChain chain) {

        Set<String> productNames = products.stream().map(Product::getProductName).collect(Collectors.toSet());

        // Schedule job
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.SIP_GENERATION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, chain.getId()),
                              new JobParameter(SIPGenerationJob.PRODUCT_NAMES, productNames));
        jobInfo.setClassName(SIPGenerationJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());
        jobInfo = jobInfoService.createAsPending(jobInfo);

        // Release lock
        for (Product product : products) {
            if (product.getLastSIPGenerationJobInfo() != null) {
                jobInfoService.unlock(product.getLastSIPGenerationJobInfo());
            }

            // Change product SIP state
            product.setSipState(ProductSIPState.SCHEDULED);
            product.setLastSIPGenerationJobInfo(jobInfo);
            save(product);
        }

        jobInfo.updateStatus(JobStatus.QUEUED);
        jobInfoService.save(jobInfo);

        return jobInfo;
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
    public Set<Product> linkAcquisitionFilesToProducts(AcquisitionProcessingChain processingChain,
            List<AcquisitionFile> validFiles) throws ModuleException {

        // Get product plugin
        IProductPlugin productPlugin = pluginService.getPlugin(processingChain.getProductPluginConf().getId());

        // Compute the  list of products to create or update
        Set<String> productNames = new HashSet<>();
        Multimap<String, AcquisitionFile> validFilesByProductName = ArrayListMultimap.create();
        for (AcquisitionFile validFile : validFiles) {

            try {
                String productName = productPlugin.getProductName(validFile.getFilePath());
                if (productName != null && !productName.isEmpty()) {
                    productNames.add(productName);
                    validFilesByProductName.put(productName, validFile);
                } else {
                    // Continue silently but register error in database
                    String errorMessage = String.format("Error computing product name for file %s : %s",
                                                        validFile.getFilePath().toString(),
                                                        "Null or empty product name");
                    LOGGER.error(errorMessage);
                    validFile.setError(errorMessage);
                    validFile.setState(AcquisitionFileState.ERROR);
                    acqFileRepository.save(validFile);
                }
            } catch (ModuleException e) {
                // Continue silently but register error in database
                String errorMessage = String.format("Error computing product name for file %s : %s",
                                                    validFile.getFilePath().toString(), e.getMessage());
                LOGGER.error(errorMessage, e);
                validFile.setError(errorMessage);
                validFile.setState(AcquisitionFileState.ERROR);
                acqFileRepository.save(validFile);
            }

        }

        // Find all existing product by using one database request
        Set<Product> products = productRepository.findCompleteByProductNameIn(productNames);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductName, Function.identity()));
        Set<Product> productsToSchedule = new HashSet<>();

        // Build all current products
        for (String productName : validFilesByProductName.keySet()) {

            // Get product
            Product currentProduct = productMap.get(productName);
            if (currentProduct == null) {
                // It is a new Product, create it
                currentProduct = new Product();
                currentProduct.setProductName(productName);
                currentProduct.setProcessingChain(processingChain);
                productMap.put(productName, currentProduct);
            }

            // Fulfill product with new valid acquired files
            fulfillProduct(validFilesByProductName.get(productName), currentProduct, processingChain);

            // Store for scheduling
            if (currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED
                    && (currentProduct.getState() == ProductState.COMPLETED
                            || currentProduct.getState() == ProductState.FINISHED)) {
                LOGGER.trace("Product {} is candidate for SIP generation", currentProduct.getProductName());
                productsToSchedule.add(currentProduct);
            }
        }

        // Schedule SIP generation
        if (!productsToSchedule.isEmpty()) {
            LOGGER.debug("Scheduling SIP generation for {} product(s)", productsToSchedule.size());
            scheduleProductSIPGenerations(productsToSchedule, processingChain);
        }

        return new HashSet<>(productMap.values());
    }

    /**
     * Fulfill product with new valid acquired files
     */
    private Product fulfillProduct(Collection<AcquisitionFile> validFiles, Product currentProduct,
            AcquisitionProcessingChain processingChain) throws ModuleException {

        for (AcquisitionFile validFile : validFiles) {
            // Mark old file as superseded
            for (AcquisitionFile existing : currentProduct.getAcquisitionFiles()) {
                if (existing.getFileInfo().equals(validFile.getFileInfo())) {
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
            // Mark file as acquired
            validFile.setState(AcquisitionFileState.ACQUIRED);
            acqFileRepository.save(validFile);
        }

        currentProduct.setSipState(ProductSIPState.NOT_SCHEDULED); // Required to be re-integrated in SIP workflow
        currentProduct.setSession(processingChain.getSession().orElse(null));
        currentProduct.addAcquisitionFiles(validFiles);
        computeProductState(currentProduct);

        return save(currentProduct);
    }

    @Override
    public Page<Product> findProductsToSubmit(String ingestChain, Optional<String> session) {

        if (session.isPresent()) {
            return productRepository.findByProcessingChainIngestChainAndSessionAndSipState(ingestChain, session
                    .get(), ProductSIPState.SUBMISSION_SCHEDULED, new PageRequest(0, bulkRequestLimit));
        } else {
            return productRepository
                    .findByProcessingChainIngestChainAndSipStateOrderByIdAsc(ingestChain,
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

        // Register all chains and sessions already scheduled
        Multimap<String, String> scheduledSessionsByChain = ArrayListMultimap.create();

        // Find all products already scheduled for submission
        Page<Product> products;
        Pageable pageable = new PageRequest(0, defaultPageSize);
        do {
            products = productRepository.findBySipStateOrderByIdAsc(ProductSIPState.SUBMISSION_SCHEDULED, pageable);
            if (products.hasNext()) {
                // Prepare for new search
                pageable = products.nextPageable();
            }
            if (products.hasContent()) {
                for (Product product : products) {
                    if (!scheduledSessionsByChain.containsEntry(product.getProcessingChain().getIngestChain(),
                                                                product.getSession())) {
                        scheduledSessionsByChain.put(product.getProcessingChain().getIngestChain(),
                                                     product.getSession());
                    }
                }
            }
        } while (products.hasNext());

        // Find all products with available SIPs ready for submission
        // Reset pagination
        pageable = new PageRequest(0, bulkRequestLimit);
        Multimap<String, String> sessionsByChain = ArrayListMultimap.create();
        // Register products per ingest chain and session for reporting
        Map<String, Map<String, List<Product>>> productsPerIngestChain = new HashMap<>();

        // Just managed one page at a time
        products = productRepository.findWithLockBySipStateOrderByIdAsc(ProductSIPState.GENERATED, pageable);

        if (products.hasContent()) {

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
                }
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
                jobInfoService.createAsPending(jobInfo);

                // Link report to all related products
                linkSubmissionJobInfo(productsPerIngestChain, jobInfo, ingestChain, session);
                jobInfo.updateStatus(JobStatus.QUEUED);
                jobInfoService.save(jobInfo);
            }
        }
    }

    /**
     * Register product
     * @param productsPerIngestChain list of product per session per ingest chain
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
                    product.setSipState(ProductSIPState.SUBMISSION_SCHEDULED);
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
    public void handleSIPSubmissiontError(JobInfo jobInfo) {
        Map<String, JobParameter> params = jobInfo.getParametersAsMap();
        String ingestChain = params.get(SIPSubmissionJob.INGEST_CHAIN_PARAMETER).getValue();
        String session = params.get(SIPSubmissionJob.SESSION_PARAMETER).getValue();
        // Update product status
        Page<Product> products;
        do {
            if (session == null) {
                products = productRepository
                        .findByProcessingChainIngestChainAndSipStateOrderByIdAsc(ingestChain,
                                                                                 ProductSIPState.SUBMISSION_SCHEDULED,
                                                                                 new PageRequest(0, defaultPageSize));
            } else {
                products = productRepository
                        .findByProcessingChainIngestChainAndSessionAndSipState(ingestChain, session,
                                                                               ProductSIPState.SUBMISSION_SCHEDULED,
                                                                               new PageRequest(0, defaultPageSize));
            }
            if (products.hasContent()) {
                for (Product product : products) {
                    product.setSipState(ProductSIPState.SUBMISSION_ERROR);
                    save(product);
                }
            }
        } while (products.hasNext());
    }

    @Override
    public void handleSIPGenerationError(JobInfo jobInfo) {
        JobParameter productNameParam = jobInfo.getParametersAsMap().get(SIPGenerationJob.PRODUCT_NAMES);
        if (productNameParam != null) {
            Set<String> productNames = productNameParam.getValue();
            try {
                Set<Product> products = retrieve(productNames);
                for (Product product : products) {
                    product.setSipState(ProductSIPState.GENERATION_ERROR);
                    product.setError(jobInfo.getStatus().getStackTrace());
                    save(product);
                }
            } catch (ModuleException e) {
                LOGGER.error("Error handling SIP generation error", e);
            }
        }
    }

    @Override
    public void handleSIPEvent(SIPEvent event) {
        Product product = productRepository.findCompleteByProductName(event.getProviderId());
        if (product != null) {
            // Do post processing if SIP properly stored
            if (SIPState.STORED.equals(event.getState())
                    && product.getProcessingChain().getPostProcessSipPluginConf().isPresent()) {
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
            product.setIpId(event.getSipId());
            save(product);
        } else {
            LOGGER.warn("SIP with IP ID \"{}\" and provider ID \"{}\" is not managed by data provider",
                        event.getSipId(), event.getProviderId());
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
    public long countSIPGenerationJobInfoByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            ISipState productSipState) {
        return productRepository
                .countDistinctLastSIPGenerationJobInfoByProcessingChainAndSipState(processingChain,
                                                                                   productSipState.toString());
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

        // Stop SIP generation jobs
        Set<JobInfo> jobInfos = productRepository
                .findDistinctLastSIPGenerationJobInfoByProcessingChainAndSipStateIn(processingChain,
                                                                                    ProductSIPState.SCHEDULED);
        jobInfos.forEach(j -> jobInfoService.stopJob(j.getId()));

        // Stop submission jobs
        jobInfos = productRepository
                .findDistinctLastSIPSubmissionJobInfoByProcessingChainAndSipStateIn(processingChain,
                                                                                    ProductSIPState.SUBMISSION_SCHEDULED);
        jobInfos.forEach(j -> jobInfoService.stopJob(j.getId()));
    }

    @Override
    public boolean isProductJobStoppedAndCleaned(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Handle SIP generation jobs
        Page<Product> products;
        Pageable pageable = new PageRequest(0, defaultPageSize);
        do {
            products = productRepository.findWithLockByProcessingChainAndSipStateOrderByIdAsc(processingChain,
                                                                                              ProductSIPState.SCHEDULED,
                                                                                              pageable);
            if (products.hasNext()) {
                pageable = products.nextPageable();
            }
            for (Product product : products) {
                if (!product.getLastSIPGenerationJobInfo().getStatus().getStatus().isFinished()) {
                    return false;
                } else {
                    // Clean product state
                    product.setSipState(ProductSIPState.SCHEDULED_INTERRUPTED);
                    save(product);
                }
            }
        } while (products.hasNext());

        // Submission cannot be stop as schedule is going on ...
        // Handle SIP submission jobs
        //        pageable = new PageRequest(0, defaultPageSize);
        //        do {
        //            products = productRepository
        //                    .findWithLockByProcessingChainAndSipStateOrderByIdAsc(processingChain,
        //                                                                          ProductSIPState.SUBMISSION_SCHEDULED,
        //                                                                          pageable);
        //            if (products.hasNext()) {
        //                pageable = products.nextPageable();
        //            }
        //            for (Product product : products) {
        //                if (!product.getLastSIPSubmissionJobInfo().getStatus().getStatus().isFinished()) {
        //                    return false;
        //                } else {
        //                    // Clean product state
        //                    product.setSipState(ProductSIPState.GENERATED);
        //                    LOGGER.debug("Saving product \"{}\" \"{}\" with IP ID \"{}\" and SIP state \"{}\"",
        //                                 product.getProductName(), product.getSip().getId(), product.getIpId(),
        //                                 product.getSipState());
        //                    save(product);
        //                }
        //            }
        //        } while (products.hasNext());

        return true;
    }

    @Override
    public boolean restartInterruptedJobsByPage(AcquisitionProcessingChain processingChain) {

        Page<Product> products = productRepository
                .findByProcessingChainAndSipStateOrderByIdAsc(processingChain, ProductSIPState.SCHEDULED_INTERRUPTED,
                                                              new PageRequest(0, defaultPageSize));
        // Schedule SIP generation
        if (products.hasContent()) {
            LOGGER.debug("Restarting interrupted SIP generation for {} product(s)", products.getContent().size());
            scheduleProductSIPGenerations(new HashSet<>(products.getContent()), processingChain);
        }
        return products.hasNext();
    }

    @Override
    public boolean retrySIPGenerationByPage(AcquisitionProcessingChain processingChain) {

        Page<Product> products = productRepository
                .findByProcessingChainAndSipStateOrderByIdAsc(processingChain, ProductSIPState.GENERATION_ERROR,
                                                              new PageRequest(0, defaultPageSize));
        // Schedule SIP generation
        if (products.hasContent()) {
            LOGGER.debug("Retrying SIP generation for {} product(s)", products.getContent().size());
            scheduleProductSIPGenerations(new HashSet<>(products.getContent()), processingChain);
        }
        return products.hasNext();
    }

    @Override
    public void updateSipStates(AcquisitionProcessingChain processingChain, ISipState fromStatus, ISipState toStatus) {
        productRepository.updateSipStates(fromStatus, toStatus, processingChain);
    }

    @Override
    public void updateSipStatesByProductNameIn(ISipState state, Set<String> productNames) {
        Assert.notNull(productNames, "Product names is required");
        productRepository.updateSipStatesByProductNameIn(state, productNames);
    }
}
