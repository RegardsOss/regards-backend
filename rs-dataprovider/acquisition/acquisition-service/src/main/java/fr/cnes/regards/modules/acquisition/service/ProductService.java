/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Arrays;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.ProductSpecifications;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.ProductsPage;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.exception.SIPGenerationException;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.DeleteProductsJob;
import fr.cnes.regards.modules.acquisition.service.job.PostAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.session.SessionChangingStateProbe;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.client.IngestClientException;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;

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
    private IAcquisitionProcessingChainRepository acqChainRepository;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IIngestClient ingestClient;

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
        LOGGER.trace("Saving product \"{}\" with IP ID \"{}\" and SIP state \"{}\"", product.getProductName(),
                     product.getIpId(), product.getSipState());
        product.setLastUpdate(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @Override
    public void save(Collection<Product> products) {
        LOGGER.trace("Saving {} products", products.size());
        products.stream().forEach(this::save);
    }

    @Override
    public Product saveAndSubmitSIP(Product product, AcquisitionProcessingChain acquisitionChain)
            throws SIPGenerationException {
        LOGGER.trace("Saving and submitting product \"{}\" with IP ID \"{}\" and SIP state \"{}\"",
                     product.getProductName(), product.getIpId(), product.getSipState());

        List<StorageMetadata> storageList = new ArrayList<>();
        for (StorageMetadataProvider storage : acquisitionChain.getStorages()) {
            storageList.add(StorageMetadata.build(storage.getPluginBusinessId(), storage.getStorePath(),
                                                  storage.getTargetTypes()));
        }

        IngestMetadataDto ingestMetadata = IngestMetadataDto
                .build(product.getProcessingChain().getLabel(), product.getSession(),
                       product.getProcessingChain().getIngestChain(), acquisitionChain.getCategories(), storageList);
        try {
            ingestClient.ingest(ingestMetadata, product.getSip());
            return save(product);
        } catch (IngestClientException e) {
            throw new SIPGenerationException(e.getMessage(), e);
        }
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
    public Set<Product> retrieve(Collection<String> productNames) {
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
    public void delete(AcquisitionProcessingChain chain, Product product) {
        productRepository.delete(product);
        sessionNotifier.notifyProductDeleted(chain.getLabel(), product);
    }

    @Override
    public long deleteBySession(AcquisitionProcessingChain chain, String session) {
        Pageable page = PageRequest.of(0, 10_000);
        Page<Product> results;
        do {
            results = productRepository.findByProcessingChainAndSession(chain, session, page);
            self.deleteProducts(chain, results.getContent());
        } while (results.hasNext());
        return results.getTotalElements();
    }

    @Override
    public long deleteByProcessingChain(AcquisitionProcessingChain chain) {
        Pageable page = PageRequest.of(0, 10_000);
        Page<Product> results;
        do {
            results = productRepository.findByProcessingChain(chain, page);
            self.deleteProducts(chain, results.getContent());

        } while (results.hasNext());
        return results.getTotalElements();
    }

    @Override
    public void deleteProducts(AcquisitionProcessingChain chain, Collection<Product> products) {
        for (Product product : products) {
            sessionNotifier.notifyProductDeleted(chain.getLabel(), product);
        }
        productRepository.deleteAll(products);
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
            sessionNotifier.notifyChangeProductState(product, ProductSIPState.SCHEDULED);
            // Change product SIP state
            product.setSipState(ProductSIPState.SCHEDULED);
            product.setLastSIPGenerationJobInfo(jobInfo);
            save(product);
        }

        jobInfo.updateStatus(JobStatus.QUEUED);
        jobInfoService.save(jobInfo);

        return jobInfo;
    }

    /**
     * Update the new product state.
     * <ul>
     *   <li> COMPLETED : If all files needed are acquired</li>
     *   <li> FINISHED  : If all files needed are acquired including the optional ones</li>
     *   <li> UPDATED   : If product was complete or finished before the new file acquired</li>
     *   <li> INVALID   : If there is too many files acquired. In this case the sipState is set to NOT_SCHEDULED_INVALID</li>
     * </ul>
     * @param product product to calculate new state
     */
    private void computeProductStateWhenNewFile(Product product) {
        // We have two cases to handle:
        // 1. Product has not yet been finished or completed
        if ((product.getState() != ProductState.FINISHED) && (product.getState() != ProductState.COMPLETED)) {
            // At least one mandatory file is VALID
            product.setState(ProductState.ACQUIRING);
            computeProductState(product);
        } else {
            // 2. product has already been completed or finished so we have to use UPDATED state
            // to handle only once all file pages have been analysed to avoid generating multiple SIPs
            product.setState(ProductState.UPDATED);
        }
    }

    /**
     * Calculate state of the given product by checking if all files needed are acquired.
     * <ul>
     *   <li> COMPLETED : If all files needed are acquired</li>
     *   <li> FINISHED  : If all files needed are acquired including the optional ones</li>
     *   <li> INVALID   : If there is too many files acquired. In this case the sipState is set to NOT_SCHEDULED_INVALID</li>
     * </ul>
     * @param product product to calculate new state
     */
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
                product.setError(String
                        .format("This product should only have %s optional files according to configuration. We found %s files matching. Please check your configuration and reacquire.",
                                nbExpectedMandatory, nbActualMandatory));
            }
        } else if (nbActualMandatory >= nbExpectedMandatory) {
            product.setState(ProductState.INVALID);
            // Propagate to SIP state
            product.setSipState(ProductSIPState.NOT_SCHEDULED_INVALID);
            product.setError(String
                    .format("This product should only have %s mandatory files according to configuration. We found %s files matching. Please check your configuration and reacquire.",
                            nbExpectedMandatory, nbActualMandatory));
        }
    }

    @Override
    public Set<Product> linkAcquisitionFilesToProducts(AcquisitionProcessingChain processingChain, String session,
            List<AcquisitionFile> validFiles) throws ModuleException {

        // Get product plugin
        IProductPlugin productPlugin;
        try {
            productPlugin = pluginService.getPlugin(processingChain.getProductPluginConf().getBusinessId());
        } catch (NotAvailablePluginConfigurationException e1) {
            throw new ModuleException("Unable to run product generation for disabled acquisition chain.", e1);
        }

        // Compute the  list of products to create or update
        Multimap<String, AcquisitionFile> validFilesByProductName = ArrayListMultimap.create();
        for (AcquisitionFile validFile : validFiles) {

            try {
                String productName = productPlugin.getProductName(validFile.getFilePath());
                if ((productName != null) && !productName.isEmpty()) {
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
        Set<Product> products = productRepository.findByProductNameIn(validFilesByProductName.keys());
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductName, Function.identity()));
        Set<Product> productsToSchedule = new HashSet<>();

        // Build all current products
        for (String productName : validFilesByProductName.keySet()) {
            Collection<AcquisitionFile> productNewValidFiles = validFilesByProductName.get(productName);
            // Get product
            Product currentProduct = productMap.get(productName);
            SessionChangingStateProbe changingStateProbe = SessionChangingStateProbe.build(currentProduct);
            if (currentProduct == null) {
                // It is a new Product, create it
                currentProduct = new Product();
                currentProduct.setProductName(productName);
                currentProduct.setProcessingChain(processingChain);
                currentProduct.setSession(session);
                productMap.put(productName, currentProduct);
            } else if (!currentProduct.getSession().equals(session)) {
                // The product is now managed by another session
                currentProduct.setSession(session);
            }

            // Fulfill product with new valid acquired files
            // After this method for each product associated to new files scanned :
            // sipState = NOT_SCHEDULED
            // productState =
            //                ACQUIRING : If product is not complete (missing files)
            //                COMPLETED : If product is complete (without optional)
            //                FINISHED  : If product is complete (with optional included)
            //                UPDATED   : If product was complete before the new file acquired.
            fulfillProduct(productNewValidFiles, currentProduct);

            // Store for scheduling
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED)
                    && ((currentProduct.getState() == ProductState.COMPLETED)
                            || (currentProduct.getState() == ProductState.FINISHED))) {
                LOGGER.trace("Product {} is candidate for SIP generation", currentProduct.getProductName());
                productsToSchedule.add(currentProduct);
            }
            changingStateProbe.addUpdatedProduct(currentProduct);
            // Notify about the product state change
            sessionNotifier.notifyChangeProductState(changingStateProbe);
        }

        // Schedule SIP generation
        if (!productsToSchedule.isEmpty()) {
            LOGGER.debug("Scheduling SIP generation for {} product(s)", productsToSchedule.size());
            scheduleProductSIPGenerations(productsToSchedule, processingChain);
        }

        return new HashSet<>(productMap.values());
    }

    /**
     * Fulfill product with new valid acquired files<br/>
     * After this method for each product associated to new files scanned :
     * <ul>
     *   <li>sipState = NOT_SCHEDULED</li>
     *   <li>productState =
     *      <ul>
     *         <li>ACQUIRING : If product is not complete (missing files)</li>
     *         <li>COMPLETED : If product is complete (without optional)</li>
     *         <li>FINISHED  : If product is complete (with optional included)</li>
     *         <li>UPDATED   : If product was complete before the new file acquired.</li>
     *       </ul>
     *   </li>
     *  </ul>
     *  @param validFiles new files acquired for the product to handle
     *  @param currentProduct product to handle
     */
    private Product fulfillProduct(Collection<AcquisitionFile> validFiles, Product currentProduct) {
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
        Long chainId = jobInfo.getParametersAsMap().get(SIPGenerationJob.CHAIN_PARAMETER_ID).getValue();
        Optional<AcquisitionProcessingChain> processingChain = acqChainRepository.findById(chainId);
        JobParameter productNameParam = jobInfo.getParametersAsMap().get(SIPGenerationJob.PRODUCT_NAMES);
        if (productNameParam != null) {
            Set<String> productNames = productNameParam.getValue();
            Set<Product> products = retrieve(productNames);
            for (Product product : products) {
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.GENERATION_ERROR);
                product.setSipState(ProductSIPState.GENERATION_ERROR);
                product.setError(jobInfo.getStatus().getStackTrace());
                save(product);
            }
            if (processingChain.isPresent()) {
                handleSipGenerationEnd(processingChain.get(), products);
            }
        }

    }

    @Override
    public void handleSipGenerationSuccess(JobInfo jobInfo) {
        Long chainId = jobInfo.getParametersAsMap().get(SIPGenerationJob.CHAIN_PARAMETER_ID).getValue();
        Optional<AcquisitionProcessingChain> processingChain = acqChainRepository.findById(chainId);
        Set<String> productNames = jobInfo.getParametersAsMap().get(SIPGenerationJob.PRODUCT_NAMES).getValue();
        Set<Product> products = retrieve(productNames);
        handleSipGenerationEnd(processingChain.get(), products);
    }

    public void handleSipGenerationEnd(AcquisitionProcessingChain chain, Collection<Product> products) {
        Set<String> sessions = products.stream().map(Product::getSession).collect(Collectors.toSet());
        for (String session : sessions) {
            if (!existsByProcessingChainAndSipStateIn(chain, ProductSIPState.SCHEDULED)) {
                sessionNotifier.notifyEndingChain(chain.getLabel(), session);
            }
        }
    }

    @Override
    public void handleIngestedSIPSuccess(Collection<RequestInfo> infos) {
        Set<Product> products = productRepository
                .findByProductNameIn(infos.stream().map(RequestInfo::getProviderId).collect(Collectors.toSet()));
        for (RequestInfo info : infos) {
            Optional<Product> oProduct = products.stream().filter(p -> p.getProductName().equals(info.getProviderId()))
                    .findFirst();
            if (oProduct.isPresent()) {
                Product product = oProduct.get();
                // Do post processing when SIP properly stored
                if (product.getProcessingChain().getPostProcessSipPluginConf().isPresent()) {
                    JobInfo jobInfo = new JobInfo(true);
                    jobInfo.setPriority(AcquisitionJobPriority.POST_ACQUISITION_JOB_PRIORITY.getPriority());
                    jobInfo.setParameters(new JobParameter(PostAcquisitionJob.EVENT_PARAMETER, info));
                    jobInfo.setClassName(PostAcquisitionJob.class.getName());
                    jobInfo.setOwner(authResolver.getUser());
                    jobInfo = jobInfoService.createAsQueued(jobInfo);

                    // Release lock
                    if (product.getLastPostProductionJobInfo() != null) {
                        jobInfoService.unlock(product.getLastPostProductionJobInfo());
                    }
                    product.setLastPostProductionJobInfo(jobInfo);
                }
                // Notification must be before the state is changed as the notifier use the current
                // state to decrement/increment session properties
                sessionNotifier.notifyChangeProductState(product, SIPState.INGESTED);
                product.setSipState(SIPState.INGESTED);
                product.setIpId(info.getSipId());
                save(product);
            } else {
                LOGGER.warn("SIP with IP ID \"{}\" and provider ID \"{}\" is not managed by data provider",
                            info.getSipId(), info.getProviderId());
            }
        }
    }

    @Override
    public void handleIngestedSIPFailed(Collection<RequestInfo> infos) {
        Set<Product> products = productRepository
                .findByProductNameIn(infos.stream().map(RequestInfo::getProviderId).collect(Collectors.toSet()));
        for (RequestInfo info : infos) {
            Optional<Product> oProduct = products.stream().filter(p -> p.getProductName().equals(info.getProviderId()))
                    .findFirst();
            if (oProduct.isPresent()) {
                Product product = oProduct.get();
                // Do post processing when SIP properly stored
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Ingestion failed with following error :  \\n");
                for (String error : info.getErrors()) {
                    errorMessage.append(error);
                    errorMessage.append("  \\n");
                }
                // Notification must be before the state is changed as the notifier use the current
                // state to decrement/increment session properties
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.INGESTION_FAILED);
                product.setSipState(ProductSIPState.INGESTION_FAILED);
                product.setIpId(info.getSipId());
                product.setError(errorMessage.toString());
                // Ensure the production job is locked
                if (product.getLastPostProductionJobInfo() != null) {
                    jobInfoService.lock(product.getLastPostProductionJobInfo());
                }
                save(product);
            } else {
                LOGGER.warn("SIP with IP ID \"{}\" and provider ID \"{}\" is not managed by data provider",
                            info.getSipId(), info.getProviderId());
            }
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
    public boolean existsByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            ISipState productSipState) {
        return productRepository.existsByProcessingChainAndSipState(processingChain, productSipState);
    }

    @Override
    public long countByChain(AcquisitionProcessingChain chain) {
        return productRepository.countByProcessingChain(chain);
    }

    @Override
    public Page<Product> search(List<ProductState> state, List<ISipState> sipState, String productName, String session,
            Long processingChainId, OffsetDateTime from, Boolean noSession, Pageable pageable) {
        return productRepository.loadAll(ProductSpecifications.search(state, sipState, productName, session,
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

        Page<Product> products = productRepository
                .findByProcessingChainAndSipStateOrderByIdAsc(processingChain, ProductSIPState.SCHEDULED_INTERRUPTED,
                                                              PageRequest.of(0, AcquisitionProperties.WORKING_UNIT));
        // Schedule SIP generation
        if (products.hasContent()) {
            LOGGER.debug("Restarting interrupted SIP generation for {} product(s)", products.getContent().size());
            scheduleProductSIPGenerations(new HashSet<>(products.getContent()), processingChain);
        }
        return products.hasNext();
    }

    @Override
    public boolean retrySIPGenerationByPage(AcquisitionProcessingChain processingChain,
            Optional<String> sessionToRetry) {

        Page<Product> products;
        if (!sessionToRetry.isPresent()) {
            products = productRepository
                    .findByProcessingChainAndSipStateInOrderByIdAsc(processingChain,
                                                                    Arrays.asList(ProductSIPState.GENERATION_ERROR,
                                                                                  ProductSIPState.INGESTION_FAILED),
                                                                    PageRequest.of(0,
                                                                                   AcquisitionProperties.WORKING_UNIT));
        } else {
            products = productRepository
                    .findByProcessingChainAndSessionAndSipStateInOrderByIdAsc(processingChain, sessionToRetry
                            .get(), Arrays.asList(ProductSIPState.GENERATION_ERROR, ProductSIPState.INGESTION_FAILED),
                                                                              PageRequest
                                                                                      .of(0,
                                                                                          AcquisitionProperties.WORKING_UNIT));
        }

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
    public long manageUpdatedProducts(AcquisitionProcessingChain processingChain) {
        ProductsPage page;
        long totalProductScheduled = 0L;
        do {
            page = self.manageUpdatedProductsByPage(processingChain);
            totalProductScheduled += page.getScheduled();
        } while (!Thread.currentThread().isInterrupted() && page.hasNext());
        // Just trace interruption
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
        return totalProductScheduled;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public ProductsPage manageUpdatedProductsByPage(AcquisitionProcessingChain processingChain) {
        // - Retrieve first page
        Page<Product> page = productRepository
                .findByProcessingChainAndStateOrderByIdAsc(processingChain, ProductState.UPDATED,
                                                           PageRequest.of(0, AcquisitionProperties.WORKING_UNIT));
        Set<Product> productsToSchedule = new HashSet<>();
        for (Product currentProduct : page.getContent()) {
            computeProductState(currentProduct);
            // Store for scheduling
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED)
                    && ((currentProduct.getState() == ProductState.COMPLETED)
                            || (currentProduct.getState() == ProductState.FINISHED))) {
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
        return ProductsPage.build(page.hasNext(), productsToSchedule.size(),
                                  page.getNumberOfElements() - productsToSchedule.size());
    }

    @Override
    public void handleGeneratedProducts(AcquisitionProcessingChain processingChain, Set<Product> success,
            Set<Product> errors) {
        for (Product product : success) {
            try {
                saveAndSubmitSIP(product, processingChain);
            } catch (SIPGenerationException e) {
                LOGGER.error(e.getMessage(), e);
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.INGESTION_FAILED);
                product.setSipState(ProductSIPState.INGESTION_FAILED);
                product.setError(e.getMessage());
                save(product);
            }
        }
        for (Product product : errors) {
            save(product);
        }
    }

    @Override
    public JobInfo scheduleProductsDeletionJob(AcquisitionProcessingChain chain, Optional<String> session,
            boolean deleteChain) {
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.DELETION_JOB.getPriority());
        jobInfo.setParameters(DeleteProductsJob.getParameters(chain.getId(), session, deleteChain));
        jobInfo.setClassName(DeleteProductsJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        return jobInfo;

    }

}