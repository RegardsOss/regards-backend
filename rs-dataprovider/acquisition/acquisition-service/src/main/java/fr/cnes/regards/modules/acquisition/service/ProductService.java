/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.ProductSpecifications;
import fr.cnes.regards.modules.acquisition.domain.*;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manage acquisition {@link Product}
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@MultitenantTransactional
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProductService implements IProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private final IPluginService pluginService;

    private final IProductRepository productRepository;

    private final IAcquisitionProcessingChainRepository acqChainRepository;

    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    private final IAcquisitionFileRepository acqFileRepository;

    private final IIngestClient ingestClient;

    private final IProductService self;

    private final SessionNotifier sessionNotifier;

    private final AcquisitionNotificationService acquisitionNotificationService;

    @Value("${regards.acquisition.product.bulk.deletion.limit:100}")
    private Integer bulkDeletionLimit;

    /**
     * All transactions only manage at most x entities at a time
     * in order to take care of the memory consumption and potential tenant starvation.
     */
    @Value("${regards.acquisition.batch.size:100}")
    private Integer bulkAcquisitionLimit;

    public ProductService(IPluginService pluginService,
                          IProductRepository productRepository,
                          IAcquisitionProcessingChainRepository acqChainRepository,
                          IAuthenticationResolver authResolver,
                          IJobInfoService jobInfoService,
                          IAcquisitionFileRepository acqFileRepository,
                          IIngestClient ingestClient,
                          IProductService productService,
                          SessionNotifier sessionNotifier,
                          AcquisitionNotificationService acquisitionNotificationService) {
        this.pluginService = pluginService;
        this.productRepository = productRepository;
        this.acqChainRepository = acqChainRepository;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
        this.acqFileRepository = acqFileRepository;
        this.ingestClient = ingestClient;
        this.self = productService;
        this.sessionNotifier = sessionNotifier;
        this.acquisitionNotificationService = acquisitionNotificationService;
    }

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
    public void save(Collection<Product> products) {
        LOGGER.trace("Saving {} products", products.size());
        products.stream().forEach(this::save);
    }

    @Override
    public Product saveAndSubmitSIP(Product product, AcquisitionProcessingChain acquisitionChain)
        throws SIPGenerationException {
        LOGGER.trace("Saving and submitting product \"{}\" with IP ID \"{}\" and SIP state \"{}\"",
                     product.getProductName(),
                     product.getIpId(),
                     product.getSipState());

        List<StorageMetadata> storageList = new ArrayList<>();

        // If products have to be stored physically, save storage paths in storageList
        // else if they have to be referenced, save them in storage location content information
        if (acquisitionChain.isProductsStored()) {
            for (StorageMetadataProvider storage : acquisitionChain.getStorages()) {
                storageList.add(StorageMetadata.build(storage.getPluginBusinessId(),
                                                      storage.getStorePath(),
                                                      storage.getTargetTypes()));
            }
        } else {
            List<ContentInformation> productCIList = product.getSip().getProperties().getContentInformations();
            for (ContentInformation productCI : productCIList) {
                if ((productCI.getDataObject() != null)
                    && (productCI.getDataObject().getLocations() != null)
                    && !productCI.getDataObject().getLocations().isEmpty()) {
                    for (OAISDataObjectLocation location : productCI.getDataObject().getLocations()) {
                        if (productCI.getDataObject().getFileSize() == null) {
                            updateDataObjectFileSize(productCI.getDataObject(), product.getProductName());
                        }
                        location.setStorage(acquisitionChain.getReferenceLocation());
                    }
                }
            }
        }

        IngestMetadataDto ingestMetadata = IngestMetadataDto.build(product.getProcessingChain().getLabel(),
                                                                   product.getSession(),
                                                                   null,
                                                                   product.getProcessingChain().getIngestChain(),
                                                                   acquisitionChain.getCategories(),
                                                                   product.getProcessingChain().getVersioningMode(),
                                                                   null,
                                                                   storageList);
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
    public void deleteBySession(AcquisitionProcessingChain chain, String session) {
        // lets try to handle products per 5 time the ordinary bulk
        Pageable page = PageRequest.of(0, bulkDeletionLimit, Sort.by("id"));
        while (!Thread.currentThread().isInterrupted() && self.deleteProducts(chain, Optional.of(session), page)) {
            // do not call page.next() here. We are asking for deletion of the products so we have to only request the first page at each iteration.
        }
    }

    @Override
    public void deleteByProcessingChain(AcquisitionProcessingChain chain) {
        Pageable page = PageRequest.of(0, bulkDeletionLimit, Sort.by("id"));
        while (!Thread.currentThread().isInterrupted() && self.deleteProducts(chain, Optional.empty(), page)) {
            // do not call page.next() here. We are asking for deletion of the products so we have to only request the first page at each iteration.
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteProducts(AcquisitionProcessingChain chain, Optional<String> session, Pageable page) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Finding products to delete for chain {} and session {}",
                     chain.getLabel(),
                     session.orElse("<none>"));
        Page<Product> productPage;
        if (session.isPresent()) {
            productPage = productRepository.findByProcessingChainAndSession(chain, session.get(), page);
        } else {
            productPage = productRepository.findByProcessingChain(chain, page);
        }
        LOGGER.debug("Found {} products to delete in {} ms",
                     productPage.getSize(),
                     System.currentTimeMillis() - startTime);
        if (productPage.hasContent()) {
            Set<Long> productIds = new HashSet<>();
            for (Product product : productPage) {
                sessionNotifier.notifyProductDeleted(chain.getLabel(), product);
                sessionNotifier.notifyFileAcquiredDeleted(product.getSession(),
                                                          chain.getLabel(),
                                                          product.getAcquisitionFiles().size());
                productIds.add(product.getId());
            }
            startTime = System.currentTimeMillis();
            // delete by product ids
            // first acquisition files that are linked to them
            acqFileRepository.deleteByProductIdIn(productIds);
            // then products themselves
            productRepository.deleteByIdIn(productIds);
            LOGGER.debug("Deleted {} products in {} ms", productPage.getSize(), System.currentTimeMillis() - startTime);
        }
        return productPage.hasNext();
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
        jobInfo.setPriority(AcquisitionJobPriority.SIP_GENERATION_JOB_PRIORITY);
        jobInfo.setParameters(new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, chain.getId()),
                              new JobParameter(SIPGenerationJob.PRODUCT_NAMES, productNames));
        jobInfo.setClassName(SIPGenerationJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());
        jobInfo = jobInfoService.createAsPending(jobInfo);

        // Start all session associated to products scheduled

        // Release lock
        for (Product product : products) {
            if (product.getLastSIPGenerationJobInfo() != null) {
                jobInfoService.unlock(product.getLastSIPGenerationJobInfo());
            }
            sessionNotifier.notifyChangeProductState(product, ProductSIPState.SCHEDULED, true);
            // Change product SIP state
            product.setSipState(ProductSIPState.SCHEDULED);
            product.setLastSIPGenerationJobInfo(jobInfo);
            save(product);
        }

        jobInfo.updateStatus(JobStatus.QUEUED);
        jobInfoService.save(jobInfo);

        handleSipGenerationStart(chain, products);

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
     *
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
     *
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

        if (nbActualMandatory > nbExpectedMandatory || nbActualOptional > nbExpectedOptional) {
            handleProductInvalidTooManyFiles(product,
                                             String.format(
                                                 "Invalid product files count. %s/%s mandatory files and %s/%s optional files found"
                                                 + "Please check your configuration and reacquire.",
                                                 nbActualMandatory,
                                                 nbExpectedMandatory,
                                                 nbActualOptional,
                                                 nbExpectedOptional));
        } else if (nbExpectedMandatory == nbActualMandatory) {
            // ProductStatus is COMPLETED if mandatory files is acquired
            product.setState(ProductState.COMPLETED);
            // Remove error if there was one previously
            product.setError(null);
            if (nbExpectedOptional == nbActualOptional) {
                // ProductStatus is FINISHED if mandatory and optional files is acquired
                product.setState(ProductState.FINISHED);
            }
        }
    }

    /**
     * Update product and associated acquisition files to error state in case of product contains too many files for
     * the associated configuration.
     */
    private void handleProductInvalidTooManyFiles(Product product, String errorCause) {
        product.setState(ProductState.INVALID);
        product.setSipState(ProductSIPState.NOT_SCHEDULED_INVALID);
        product.setError(errorCause);
        product.getAcquisitionFiles().forEach(acqFile -> {
            acqFile.setErrorMsgWithState(String.format("File has been invalidated cause its product owner %s is "
                                                       + "invalid. Product contains too much files.",
                                                       product.getProductName()), AcquisitionFileState.ERROR);
        });
    }

    @Override
    public Set<Product> linkAcquisitionFilesToProducts(AcquisitionProcessingChain processingChain,
                                                       String session,
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
                if (!StringUtils.isBlank(productName)) {
                    validFilesByProductName.put(productName, validFile);
                } else {
                    // Continue silently but register error in database
                    String errorMessage = String.format("Error computing product name for file %s : %s",
                                                        validFile.getFilePath().toString(),
                                                        "Null or empty product name");
                    LOGGER.error(errorMessage);
                    validFile.setErrorMsgWithState(errorMessage, AcquisitionFileState.ERROR);
                    acqFileRepository.save(validFile);
                }
            } catch (ModuleException e) {
                // Continue silently but register error in database
                String errorMessage = String.format("Error computing product name for file %s : %s",
                                                    validFile.getFilePath().toString(),
                                                    e.getMessage());
                LOGGER.error(errorMessage, e);
                validFile.setErrorMsgWithState(errorMessage, AcquisitionFileState.ERROR);
                acqFileRepository.save(validFile);
            }

        }

        // Find all existing product by using one database request
        Set<Product> products = productRepository.findByProductNameIn(validFilesByProductName.keys());
        Map<String, Product> productMap = products.stream()
                                                  .collect(Collectors.toMap(Product::getProductName,
                                                                            Function.identity()));
        Set<Product> productsToSchedule = new HashSet<>();
        List<AcquisitionFile> invalidFiles = new ArrayList<>();

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
            } else if (!currentProduct.getProcessingChain().equals(processingChain)) {
                // this case is forbidden because it breaks everything
                // files are then declared invalid and product never created
                for (AcquisitionFile invalidFile : productNewValidFiles) {
                    invalidFile.setErrorMsgWithState(String.format(
                        "This file should generate a product(name: %s) which has been created "
                        + "by another chain %s. So it is invalid.",
                        productName,
                        currentProduct.getProcessingChain().getLabel()), AcquisitionFileState.INVALID);
                    sessionNotifier.notifyFileInvalid(session, processingChain.getLabel(), 1);

                    invalidFiles.add(invalidFile);
                }
                continue;
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
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED) && ((currentProduct.getState()
                                                                                     == ProductState.COMPLETED) || (
                                                                                        currentProduct.getState()
                                                                                        == ProductState.FINISHED))) {
                LOGGER.trace("Product {} is candidate for SIP generation", currentProduct.getProductName());
                productsToSchedule.add(currentProduct);
            }
            changingStateProbe.addUpdatedProduct(currentProduct);
            // Notify about the product state change
            sessionNotifier.notifyChangeProductState(changingStateProbe);
        }

        if (!invalidFiles.isEmpty()) {
            LOGGER.debug("{} invalid files for {} product(s)", invalidFiles.size(), productsToSchedule.size());
            acquisitionNotificationService.notifyInvalidAcquisitionFile(invalidFiles);
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
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.GENERATION_ERROR, true);
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

    @Override
    public void handleSIPGenerationAborted(JobInfo jobInfo) {
        Long chainId = jobInfo.getParametersAsMap().get(SIPGenerationJob.CHAIN_PARAMETER_ID).getValue();
        Optional<AcquisitionProcessingChain> processingChain = acqChainRepository.findById(chainId);
        JobParameter productNameParam = jobInfo.getParametersAsMap().get(SIPGenerationJob.PRODUCT_NAMES);
        if (productNameParam != null) {
            Set<String> productNames = productNameParam.getValue();
            Set<Product> products = retrieve(productNames);
            if (processingChain.isPresent()) {
                Set<String> sessions = products.stream().map(Product::getSession).collect(Collectors.toSet());
                for (String session : sessions) {
                    sessionNotifier.notifyEndingChain(processingChain.get().getLabel(), session);
                }
            }
        }
    }

    /**
     * Notify each session started by scheduled products
     */
    public void handleSipGenerationStart(AcquisitionProcessingChain chain, Collection<Product> products) {
        Set<String> sessions = products.stream().map(Product::getSession).collect(Collectors.toSet());
        for (String session : sessions) {
            sessionNotifier.notifyStartingChain(chain.getLabel(), session);
        }
    }

    public void handleSipGenerationEnd(AcquisitionProcessingChain chain, Collection<Product> products) {
        // Remove link to job_info when sip generation job ends to allow deletion of jobs done.
        products.forEach(p -> p.setLastSIPGenerationJobInfo(null));
        Set<String> sessions = products.stream().map(Product::getSession).collect(Collectors.toSet());
        for (String session : sessions) {
            sessionNotifier.notifyEndingChain(chain.getLabel(), session);
        }
    }

    @Override
    public void handleIngestedSIPDeleted(Collection<RequestInfo> infos) {
        // If ingestion request has been deleted or canceled, the product acquisition is canceled too.
        // Update product state and notify session.
        Set<Product> products = productRepository.findByProductNameIn(infos.stream()
                                                                           .map(RequestInfo::getProviderId)
                                                                           .collect(Collectors.toSet()));
        products.stream().forEach(product -> {
            sessionNotifier.notifyChangeProductState(product, SIPState.DELETED, true);
            product.setSipState(SIPState.DELETED);
            product.setLastUpdate(OffsetDateTime.now());
            product.setState(ProductState.CANCELED);
        });
        productRepository.saveAll(products);
    }

    @Override
    public void handleIngestedSIPSuccess(Collection<RequestInfo> infos) {
        Set<Product> products = productRepository.findByProductNameIn(infos.stream()
                                                                           .map(RequestInfo::getProviderId)
                                                                           .collect(Collectors.toSet()));
        for (RequestInfo info : infos) {
            Optional<Product> oProduct = products.stream()
                                                 .filter(p -> p.getProductName().equals(info.getProviderId()))
                                                 .findFirst();
            if (oProduct.isPresent()) {
                Product product = oProduct.get();
                // Do post processing when SIP properly stored
                if (product.getProcessingChain().getPostProcessSipPluginConf().isPresent()) {
                    JobInfo jobInfo = new JobInfo(true);
                    jobInfo.setPriority(AcquisitionJobPriority.POST_ACQUISITION_JOB_PRIORITY);
                    jobInfo.setParameters(new JobParameter(PostAcquisitionJob.EVENT_PARAMETER, info));
                    jobInfo.setClassName(PostAcquisitionJob.class.getName());
                    jobInfo.setOwner(authResolver.getUser());
                    jobInfo = jobInfoService.createAsQueued(jobInfo);
                    sessionNotifier.notifyStartingChain(product.getProcessingChain().getLabel(), product.getSession());

                    // Release lock
                    if (product.getLastPostProductionJobInfo() != null) {
                        jobInfoService.unlock(product.getLastPostProductionJobInfo());
                    }
                    product.setLastPostProductionJobInfo(jobInfo);
                }
                // Notification must be before the state is changed as the notifier use the current
                // state to decrement/increment session properties
                sessionNotifier.notifyChangeProductState(product,
                                                         SIPState.INGESTED,
                                                         product.getSipState() == ProductSIPState.INGESTION_FAILED);
                product.setSipState(SIPState.INGESTED);
                product.setIpId(info.getSipId());
                save(product);
            } else {
                LOGGER.warn("SIP with IP ID \"{}\" and provider ID \"{}\" is not managed by data provider",
                            info.getSipId(),
                            info.getProviderId());
            }
        }
    }

    @Override
    public void handleIngestedSIPFailed(Collection<RequestInfo> infos) {
        Set<Product> products = productRepository.findByProductNameIn(infos.stream()
                                                                           .map(RequestInfo::getProviderId)
                                                                           .collect(Collectors.toSet()));
        for (RequestInfo info : infos) {
            Optional<Product> oProduct = products.stream()
                                                 .filter(p -> p.getProductName().equals(info.getProviderId()))
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
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.INGESTION_FAILED, false);
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
                            info.getSipId(),
                            info.getProviderId());
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
        return productRepository.countDistinctLastSIPGenerationJobInfoByProcessingChainAndSipState(processingChain,
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
    public Page<Product> search(List<ProductState> state,
                                List<ISipState> sipState,
                                String productName,
                                String session,
                                Long processingChainId,
                                OffsetDateTime from,
                                Boolean noSession,
                                Pageable pageable) {
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
        Pageable pageable = PageRequest.of(0, bulkAcquisitionLimit);
        do {
            products = productRepository.findWithLockByProcessingChainAndSipStateOrderByIdAsc(processingChain,
                                                                                              ProductSIPState.SCHEDULED,
                                                                                              pageable);
            for (Product product : products) {
                if (product.getLastSIPGenerationJobInfo() != null && !product.getLastSIPGenerationJobInfo()
                                                                             .getStatus()
                                                                             .getStatus()
                                                                             .isFinished()) {
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
                                                                                                               bulkAcquisitionLimit));
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
            products = productRepository.findByProcessingChainAndSipStateInOrderByIdAsc(processingChain,
                                                                                        Arrays.asList(ProductSIPState.GENERATION_ERROR,
                                                                                                      ProductSIPState.INGESTION_FAILED),
                                                                                        PageRequest.of(0,
                                                                                                       bulkAcquisitionLimit));
        } else {
            products = productRepository.findByProcessingChainAndSessionAndSipStateInOrderByIdAsc(processingChain,
                                                                                                  sessionToRetry.get(),
                                                                                                  Arrays.asList(
                                                                                                      ProductSIPState.GENERATION_ERROR,
                                                                                                      ProductSIPState.INGESTION_FAILED),
                                                                                                  PageRequest.of(0,
                                                                                                                 bulkAcquisitionLimit));
        }

        // Schedule SIP generation
        if (products.hasContent()) {
            LOGGER.debug("Retrying SIP generation for {} product(s)", products.getContent().size());
            scheduleProductSIPGenerations(new HashSet<>(products.getContent()), processingChain);
        }
        return products.hasNext();
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
        Page<Product> page = productRepository.findByProcessingChainAndStateOrderByIdAsc(processingChain,
                                                                                         ProductState.UPDATED,
                                                                                         PageRequest.of(0,
                                                                                                        bulkAcquisitionLimit));
        Set<Product> productsToSchedule = new HashSet<>();
        for (Product currentProduct : page.getContent()) {
            computeProductState(currentProduct);
            // Store for scheduling
            if ((currentProduct.getSipState() == ProductSIPState.NOT_SCHEDULED) && ((currentProduct.getState()
                                                                                     == ProductState.COMPLETED) || (
                                                                                        currentProduct.getState()
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
        return ProductsPage.build(page.hasNext(),
                                  productsToSchedule.size(),
                                  page.getNumberOfElements() - productsToSchedule.size());
    }

    @Override
    public void handleGeneratedProducts(AcquisitionProcessingChain processingChain,
                                        Set<Product> success,
                                        Set<Product> errors) {
        for (Product product : success) {
            try {
                saveAndSubmitSIP(product, processingChain);
            } catch (SIPGenerationException e) {
                LOGGER.error(e.getMessage(), e);
                sessionNotifier.notifyChangeProductState(product, ProductSIPState.INGESTION_FAILED, false);
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
    public JobInfo scheduleProductsDeletionJob(AcquisitionProcessingChain chain,
                                               Optional<String> session,
                                               boolean deleteChain) {
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.DELETION_JOB);
        jobInfo.setParameters(DeleteProductsJob.getParameters(chain.getId(), session, deleteChain));
        jobInfo.setClassName(DeleteProductsJob.class.getName());
        jobInfo.setOwner(authResolver.getUser());
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        return jobInfo;
    }

    public Integer getBulkAcquisitionLimit() {
        return bulkAcquisitionLimit;
    }

    /**
     * Update file size of given {@link OAISDataObject} by accessing file on file system.
     *
     * @throws SIPGenerationException If file is not accessible.
     */
    private void updateDataObjectFileSize(OAISDataObject dataObject, String productName) throws SIPGenerationException {
        Optional<OAISDataObjectLocation> firstLocation = dataObject.getLocations().stream().findFirst();
        if (firstLocation.isPresent()) {
            try {
                OAISDataObjectLocation location = firstLocation.get();
                URL url = new URL(location.getUrl());
                dataObject.setFileSize(Paths.get(url.toURI()).toFile().length());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new SIPGenerationException(String.format(
                    "Unable to calculate file size for file %s in product %s.",
                    firstLocation.get().getUrl(),
                    productName), e);
            }
        }
    }
}