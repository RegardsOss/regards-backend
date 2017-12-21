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

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class AcquisitionFileService implements IAcquisitionFileService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionFileService.class);

    /**
     * {@link AcquisitionFile} repository
     */
    private final IAcquisitionFileRepository acqfileRepository;

    /**
     * {@link AcquisitionProcessingChain} service
     */
    private final IAcquisitionProcessingChainService acqProcessingChainService;

    /**
     * {@link Product} service
     */
    private final IProductService productService;

    /**
     * Constructor with the bean method's member as parameters
     *
     * @param acqFileRepository a {@link AcquisitionFile} repository
     * @param prService a {@link Product} service
     * @param acqProcessChainService a {@link AcquisitionProcessingChain} service
     */
    public AcquisitionFileService(IAcquisitionFileRepository acqFileRepository, IProductService productService,
            IAcquisitionProcessingChainService acqProcessChainService) {
        super();
        this.acqfileRepository = acqFileRepository;
        this.acqProcessingChainService = acqProcessChainService;
        this.productService = productService;
    }

    @Override
    public AcquisitionFile save(AcquisitionFile acqFile) {
        return acqfileRepository.save(acqFile);
    }

    @Override
    public Page<AcquisitionFile> retrieveAll(Pageable page) {
        return acqfileRepository.findAll(page);
    }

    @Override
    public AcquisitionFile retrieve(Long id) {
        return acqfileRepository.findOne(id);
    }

    @Override
    public void delete(Long id) {
        acqfileRepository.delete(id);
    }

    @Override
    public void delete(AcquisitionFile acquisitionFile) {
        acqfileRepository.delete(acquisitionFile);
    }

    @Override
    public List<AcquisitionFile> findByMetaFile(MetaFile metaFile) {
        return acqfileRepository.findByMetaFile(metaFile);
    }

    @Override
    public List<AcquisitionFile> findByStatus(AcquisitionFileStatus status) {
        return acqfileRepository.findByStatus(status);
    }

    @Override
    public List<AcquisitionFile> findByStatusAndMetaFile(AcquisitionFileStatus status, MetaFile metaFile) {
        return acqfileRepository.findByStatusAndMetaFile(status, metaFile);
    }

    @Override
    public List<AcquisitionFile> findByProduct(String productName) {
        return acqfileRepository.findByProductProductName(productName);
    }

    @Override
    public void saveAcqFilesAndChain(Set<AcquisitionFile> acquisitionFiles, AcquisitionProcessingChain chain)
            throws ModuleException {
        if (acquisitionFiles != null) {
            for (AcquisitionFile af : acquisitionFiles) {
                List<AcquisitionFile> listAf = this.findByMetaFile(af.getMetaFile());

                if (listAf.contains(af)) {
                    // if the AcquisitionFile already exists in database
                    // update his status and his date acquisition
                    AcquisitionFile afExisting = listAf.get(listAf.indexOf(af));
                    afExisting.setAcqDate(af.getAcqDate());
                    afExisting.setStatus(AcquisitionFileStatus.IN_PROGRESS);
                    this.save(afExisting);
                } else {
                    af.setStatus(AcquisitionFileStatus.IN_PROGRESS);
                    this.save(af);
                }

                // for the first activation of the AcquisitionProcessingChain
                // set the last activation date with the activation date of the current AcquisitionFile
                if (chain.getLastDateActivation() == null) {
                    chain.setLastDateActivation(af.getAcqDate());
                } else {
                    if ((af.getAcqDate() != null) && chain.getLastDateActivation().isBefore(af.getAcqDate())) {
                        chain.setLastDateActivation(af.getAcqDate());
                    }
                }
            }
        }

        // Save the AcquisitionProcessingChain the last activation date as been modified
        acqProcessingChainService.createOrUpdate(chain);
    }

    @Override
    public void saveAcqFileAndProduct(AcquisitionFile acqFile) {
        if (acqFile.getProduct() != null) {
            productService.save(acqFile.getProduct());
        }
        this.save(acqFile);
    }

    @Override
    public void checkFileStatus(boolean result, String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain) throws ModuleException {
        if (result) {

            LOGGER.info("Valid file {}", acqFile.getFileName());

            acqFile.setStatus(AcquisitionFileStatus.VALID);

            // Link valid file to product
            acqFile.setProduct(productService.linkAcquisitionFileToProduct(session, acqFile, productName, metaProduct,
                                                                           ingestChain));

        } else {

            // TODO CMZ à gérer le cas d'un fichier optionnel INVALID
            // juste logger que le fichier est invalide, mais le produit peut quand même être bon

            LOGGER.info("Invalid file {}", acqFile.getFileName());

            acqFile.setStatus(AcquisitionFileStatus.INVALID);
        }

        this.saveAcqFileAndProduct(acqFile);
    }

}
