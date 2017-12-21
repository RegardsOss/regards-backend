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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionProcessingChainJobParameter;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;

/**
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    /**
     * {@link Product} repository
     */
    private final IProductRepository productRepository;

    /**
     * Resolver to retrieve authentication information
     */
    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    private final IExecAcquisitionProcessingChainService execAcquisitionProcessingChainService;

    public ProductService(IProductRepository repository, IAuthenticationResolver authResolver,
            IJobInfoService jobInfoService,
            ExecAcquisitionProcessingChainService execAcquisitionProcessingChainService) {
        this.productRepository = repository;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
        this.execAcquisitionProcessingChainService = execAcquisitionProcessingChainService;
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
            LOG.error(message);
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
        return productRepository.findChainProductsToSchedule(chain.getLabel());
    }

    /**
     * Schedule a {@link SIPGenerationJob} and update product SIP state in same transaction.
     */
    @Override
    public JobInfo scheduleProductSIPGeneration(Product product, AcquisitionProcessingChain chain) {

        // Schedule job
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new AcquisitionProcessingChainJobParameter(chain),
                                  new ProductJobParameter(product.getProductName()));
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
    public Set<Product> findBySendedAndStatusIn(Boolean sended, ProductState... status) {
        return productRepository.findBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductState... status) {
        return productRepository.findDistinctIngestChainBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductState... status) {
        return productRepository.findDistinctSessionByIngestChainAndSendedAndStatusIn(ingestChain, sended, status);
    }

    @Override
    public Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session,
            Boolean sended, Pageable pageable, ProductState... status) {
        return productRepository.findAllByIngestChainAndSessionAndSendedAndStatusIn(ingestChain, session, sended,
                                                                                    pageable, status);
    }

    @Override
    public void calcProductStatus(Product product) {
        // At least one mandatory file is VALID
        product.setState(ProductState.ACQUIRING);

        if (product.getMetaProduct() == null) {
            LOG.error("[{}] The meta product of the product {} <{}> should not be null", product.getSession(),
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
                if (af.getMetaFile().equals(mf) && af.getStatus().equals(AcquisitionFileStatus.VALID)) {
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
            product.setStatus(ProductState.COMPLETED);
            if (nbTotalOptional == nbActualOptional) {
                // ProductStatus is FINISHED if mandatory and optional files is acquired
                product.setStatus(ProductState.FINISHED);
            }
        }
    }

    @Override
    public Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain) {
        // Get the product if it exists
        Product currentProduct = this.retrieve(productName);

        if (currentProduct == null) {
            // It is a new Product, create it
            currentProduct = new Product();
            currentProduct.setProductName(productName);
            currentProduct.setMetaProduct(metaProduct);
        }

        currentProduct.setSession(session);
        currentProduct.setIngestChain(ingestChain);
        currentProduct.addAcquisitionFile(acqFile);
        this.calcProductStatus(currentProduct);

        return currentProduct;
    }
}
