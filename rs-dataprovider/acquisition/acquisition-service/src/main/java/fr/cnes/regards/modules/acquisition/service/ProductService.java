/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
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
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.ingest.domain.flow.SipFlowItem;
import java.time.OffsetDateTime;
import java.util.Collection;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.Assert;

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

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IProductService self;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Value("${regards.acquisition.sip.bulk.request.limit:100}")
    private Integer bulkRequestLimit;

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public Product save(Product product) {
        LOGGER.trace("Saving product \"{}\" with IP ID \"{}\" and SIP state \"{}\"",
                     product.getProductName(),
                     product.getIpId(),
                     product.getSipState());
        product.setLastUpdate(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @Override
    public Product saveAndSubmitSIP(Product product) {
        LOGGER.trace("Saving and submitting product \"{}\" with IP ID \"{}\" and SIP state \"{}\"",
                     product.getProductName(),
                     product.getIpId(),
                     product.getSipState());
        // Build flow item
        SipFlowItem item = SipFlowItem
                .build(product.getProcessingChain().getIngestChain(), product.getSession(), product.getSip(), appName);
        publisher.publish(item);
        return save(product);
    }

    @Override
    public Product loadProduct(Long id) throws ModuleException {
        Product product = productRepository.findOneById(id);
        if (product == null) {
            throw new EntityNotFoundException(id, Product.class);
        }
        return product;
    }

    @Override
    public Set<Product> retrieve(Collection<String> productNames) throws ModuleException {
        return productRepository.findByProductNameIn(productNames);
    }

    @Override
    public Product retrieve(String productName) throws ModuleException {
        Product product = productRepository.findByProductName(productName);
        if (product == null) {
            String message = String.format("Product with name \"%s\" not found", productName);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }
        return product;
    }

    @Override
    public Optional<Product> searchProduct(String productName) throws ModuleException {
        return Optional.ofNullable(productRepository.findByProductName(productName));
    }

    @Override
    public Page<Product> retrieveAll(Pageable page) {
        return productRepository.findAll(page);
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
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

    private void computeProductStateWhenNewFile(Product product) {
        // We have two cases to handle:
        // 1. Product has not yet been finished or completed
        if (product.getState() != ProductState.FINISHED && product.getState() != ProductState.COMPLETED) {
            // At least one mandatory file is VALID
            product.setState(ProductState.ACQUIRING);

            computeProductState(product);
        } else {
            // 2. product has already been completed or finished so we have to use UPDATED state
            // to handle only once all file pages have been analysed to avoid generating multiple SIPs
            product.setState(ProductState.UPDATED);
        }
    }

    private void computeProductState(Product product) {
        int nbExpectedMandatory = 0;
        int nbExpectedOptional = 0;
        int nbActualMandatory = 0;
        int nbActualOptional = 0;

        // Compute product requirements
        for (AcquisitionFileInfo fileInfo : product.getProcessingChain().getFileInfos()) {
            if (fileInfo.isMandatory()) {
                nbExpectedMandatory++;
            } else {
                nbExpectedOptional++;
            }
        }

        // Compute product state
        for (AcquisitionFile file : product.getAcquisitionFiles()) {
            if (AcquisitionFileState.ACQUIRED.equals(file.getState())) {
                if (file.getFileInfo().isMandatory()) {
                    nbActualMandatory++;
                } else {
                    nbActualOptional++;
                }
            }
        }

        if (nbExpectedMandatory == nbActualMandatory) {
            // ProductStatus is COMPLETED if mandatory files is acquired
            product.setState(ProductState.COMPLETED);
            if (nbExpectedOptional == nbActualOptional) {
                // ProductStatus is FINISHED if mandatory and optional files is acquired
                product.setState(ProductState.FINISHED);
            } else if (nbActualOptional >= nbExpectedOptional) {
                product.setState(ProductState.INVALID);
                // Propagate to SIP state
                product.setSipState(ProductSIPState.NOT_SCHEDULED_INVALID);
                product.setError(String.format(
                        "This product should only have %s optional files according to configuration. We found %s files matching. Please check your configuration and reacquire.",
                        nbExpectedMandatory,
                        nbActualMandatory));
            }
        } else if (nbActualMandatory >= nbExpectedMandatory) {
            product.setState(ProductState.INVALID);
            // Propagate to SIP state
            product.setSipState(ProductSIPState.NOT_SCHEDULED_INVALID);
            product.setError(String.format(
                    "This product should only have %s mandatory files according to configuration. We found %s files matching. Please check your configuration and reacquire.",
                    nbExpectedMandatory,
                    nbActualMandatory));
        }
    }

    @Override
    public Set<Product> linkAcquisitionFilesToProducts(AcquisitionProcessingChain processingChain,
            List<AcquisitionFile> validFiles) throws ModuleException {

        // Get product plugin
        IProductPlugin productPlugin;
        try {
            productPlugin = pluginService.getPlugin(processingChain.getProductPluginConf().getBusinessId());
        } catch (NotAvailablePluginConfigurationException e1) {
            throw new ModuleException("Unable to run product generation for disabled acquisition chain.", e1);
        }

        // Get current session
        Map<String, JobParameter> jobsParameters = processingChain.getLastProductAcquisitionJobInfo().getParametersAsMap();
        String session = jobsParameters.get(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION).getValue();

        // Compute the  list of products to create or update
        Set<String> productNames = new HashSet<>();
        Multimap<String, AcquisitionFile> validFilesByProductName = ArrayListMultimap.create();
        for (AcquisitionFile validFile : validFiles) {

            try {
                String productName = productPlugin.getProductName(validFile.getFilePath());
                if ((productName != null) && !productName.isEmpty()) {
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
                                                    validFile.getFilePath().toString(),
                                                    e.getMessage());
                LOGGER.error(errorMessage, e);
                validFile.setError(errorMessage);
                validFile.setState(AcquisitionFileState.ERROR);
                acqFileRepository.save(validFile);
            }

        }

        // Find all existing product by using one database request
        Set<Product> products = productRepository.findByProductNameIn(productNames);
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
                currentProduct.setSession(session);
                productMap.put(productName, currentProduct);
            } else if (!currentProduct.getSession().equals(session)) {
                // The product is now managed by another session
                sessionNotifier.notifyDecrementSession(currentProduct.getProcessingChain().getLabel(), currentProduct.getSession(), currentProduct.getState());
                currentProduct.setSession(session);
            }
            // Keep the current product state if we need to send a notif
            ProductState oldState = currentProduct.getState();

            // Fulfill product with new valid acquired files
            fulfillProduct(validFilesByProductName.get(productName), currentProduct, processingChain);

            // Store for scheduling
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED) && (
                    (currentProduct.getState() == ProductState.COMPLETED) || (currentProduct.getState()
                            == ProductState.FINISHED))) {
                LOGGER.trace("Product {} is candidate for SIP generation", currentProduct.getProductName());
                productsToSchedule.add(currentProduct);
            }
            // Notify about the product state change
            sessionNotifier.notifyProductStateChanges(currentProduct, oldState);
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
        // valid product
        currentProduct.setSipState(ProductSIPState.NOT_SCHEDULED); // Required to be re-integrated in SIP workflow
        currentProduct.addAcquisitionFiles(validFiles);
        computeProductStateWhenNewFile(currentProduct);

        return save(currentProduct);
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
        Product product = productRepository.findByProductName(event.getProviderId());
        if (product != null) {
            // Do post processing if SIP properly stored
            if (SIPState.STORED.equals(event.getState()) && product.getProcessingChain().getPostProcessSipPluginConf()
                    .isPresent()) {
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
                        event.getSipId(),
                        event.getProviderId());
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
        return productRepository.countDistinctLastSIPGenerationJobInfoByProcessingChainAndSipState(processingChain,
                                                                                                   productSipState
                                                                                                           .toString());
    }

    @Override
    public long countByChain(AcquisitionProcessingChain chain) {
        return productRepository.countByProcessingChain(chain);
    }

    @Override
    public Page<Product> search(List<ProductState> state, List<ISipState> sipState, String productName, String session,
            Long processingChainId, OffsetDateTime from, Boolean noSession, Pageable pageable) {
        return productRepository.loadAll(ProductSpecifications.search(state,
                                                                      sipState,
                                                                      productName,
                                                                      session,
                                                                      processingChainId,
                                                                      from,
                                                                      noSession), pageable);
    }

    @Override
    public void stopProductJobs(AcquisitionProcessingChain processingChain) throws ModuleException {

        // Stop SIP generation jobs
        Set<JobInfo> jobInfos = productRepository.findDistinctLastSIPGenerationJobInfoByProcessingChainAndSipStateIn(
                processingChain,
                ProductSIPState.SCHEDULED);
        jobInfos.forEach(j -> jobInfoService.stopJob(j.getId()));
    }

    @Override
    public boolean isProductJobStoppedAndCleaned(AcquisitionProcessingChain processingChain) throws ModuleException {
        // Handle SIP generation jobs
        Page<Product> products;
        Pageable pageable = PageRequest.of(0, AcquisitionProperties.WORKING_UNIT);
        do {
            products = productRepository.findWithLockByProcessingChainAndSipStateOrderByIdAsc(processingChain,
                                                                                              ProductSIPState.SCHEDULED,
                                                                                              pageable);
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

        return true;
    }

    @Override
    public boolean restartInterruptedJobsByPage(AcquisitionProcessingChain processingChain) {

        Page<Product> products = productRepository.findByProcessingChainAndSipStateOrderByIdAsc(processingChain,
                                                                                                ProductSIPState.SCHEDULED_INTERRUPTED,
                                                                                                PageRequest.of(0,
                                                                                                               AcquisitionProperties.WORKING_UNIT));
        // Schedule SIP generation
        if (products.hasContent()) {
            LOGGER.debug("Restarting interrupted SIP generation for {} product(s)", products.getContent().size());
            scheduleProductSIPGenerations(new HashSet<>(products.getContent()), processingChain);
        }
        return products.hasNext();
    }

    @Override
    public boolean retrySIPGenerationByPage(AcquisitionProcessingChain processingChain) {

        Page<Product> products = productRepository.findByProcessingChainAndSipStateOrderByIdAsc(processingChain,
                                                                                                ProductSIPState.GENERATION_ERROR,
                                                                                                PageRequest.of(0,
                                                                                                               AcquisitionProperties.WORKING_UNIT));
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

    @Override
    public void manageUpdatedProducts(AcquisitionProcessingChain processingChain) {
        while (!Thread.currentThread().isInterrupted() && self.manageUpdatedProductsByPage(processingChain)) {
            // Works as long as there is at least one page left
        }
        // Just trace interruption
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean manageUpdatedProductsByPage(AcquisitionProcessingChain processingChain) {
        // - Retrieve first page
        Page<Product> page = productRepository.findByProcessingChainAndStateOrderByIdAsc(processingChain,
                                                                                         ProductState.UPDATED,
                                                                                         PageRequest.of(0,
                                                                                                        AcquisitionProperties.WORKING_UNIT));
        Set<Product> productsToSchedule = new HashSet<>();
        for (Product currentProduct : page.getContent()) {
            computeProductState(currentProduct);
            // Store for scheduling
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED) && (
                    (currentProduct.getState() == ProductState.COMPLETED) || (currentProduct.getState()
                            == ProductState.FINISHED))) {
                LOGGER.trace("Product {} is candidate for SIP generation", currentProduct.getProductName());
                productsToSchedule.add(currentProduct);
            }
        }

        // Schedule SIP generation
        if (!productsToSchedule.isEmpty()) {
            LOGGER.debug("Scheduling SIP generation for {} product(s)", productsToSchedule.size());
            scheduleProductSIPGenerations(productsToSchedule, processingChain);
        }

        productRepository.saveAll(page.getContent());
        return page.hasNext();
    }

}