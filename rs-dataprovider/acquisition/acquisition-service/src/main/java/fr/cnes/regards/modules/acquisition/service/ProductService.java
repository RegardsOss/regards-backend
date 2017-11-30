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

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProductService implements IProductService {

    /**
     * A {@link IProductRepository} bean
     */
    private final IProductRepository productRepository;

    /**
     * Constructor with the bean method's member as parameters
     * @param repository a {@link IProductRepository} bean
     */
    public ProductService(IProductRepository repository) {
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
    public Product retrieve(String productName) {
        return productRepository.findCompleteByProductName(productName);
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
    public Set<Product> findByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Override
    public Set<Product> findBySendedAndStatusIn(Boolean sended, ProductStatus... status) {
        return productRepository.findBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductStatus... status) {
        return productRepository.findDistinctIngestChainBySendedAndStatusIn(sended, status);
    }

    @Override
    public Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductStatus... status) {
        return productRepository.findDistinctSessionByIngestChainAndSendedAndStatusIn(ingestChain, sended, status);
    }

    @Override
    public Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session,
            Boolean sended, Pageable pageable, ProductStatus... status) {
        return productRepository.findAllByIngestChainAndSessionAndSendedAndStatusIn(ingestChain, session, sended,
                                                                                    pageable, status);
    }

    @Override
    public void calcProductStatus(Product product) {
        int nbTotalMandatory = 0;
        int nbTotalOptional = 0;
        int nbActualMandatory = 0;
        int nbActualOptional = 0;

        // At least one mandatory file is VALID
        product.setStatus(ProductStatus.ACQUIRING);

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
            product.setStatus(ProductStatus.COMPLETED);
            if (nbTotalOptional == nbActualOptional) {
                // ProductStatus is FINISHED if mandatory and optional files is acquired
                product.setStatus(ProductStatus.FINISHED);
            }
        }
    }

    @Override
    public Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain) {
        // Get the product if it exists
        Product currentProduct = this.retrieve(productName);

        if (currentProduct == null) {
            // It is a new Product,  create it
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

    //    @Override
    //    public void setSipAndSave(Product product, SIP sip) {
    //        product.setSip(sip);
    //        this.save(product);
    //    }
    //    
    //    @Override
    //    public void setProductAsSend(String sipId) {
    //        Product product = this.retrieve(sipId);
    //        if (product == null) {
    //            final StringBuilder buff = new StringBuilder();
    //            buff.append("The product name <");
    //            buff.append(sipId);
    //            buff.append("> does not exist");
    //            LOG.error(buff.toString());
    //        } else {
    //            product.setSended(Boolean.TRUE);
    //            this.save(product);
    //        }
    //    }

    //    @Override
    //    public void setStatusAndSaved(String sipId, ProductStatus status) {
    //        // todo cmz, il faut un retrieve light et pas complet 
    //        Product product = this.retrieve(sipId);
    //        product.setStatus(status);
    //        this.save(product);
    //    }
}
