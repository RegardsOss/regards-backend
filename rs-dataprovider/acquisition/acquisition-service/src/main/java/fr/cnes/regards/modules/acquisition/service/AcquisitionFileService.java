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

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.AcquisitionFileSpecifications;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Service to handle {@link AcquisitionFile}
 * @author Sébastien Binda
 *
 */
@MultitenantTransactional
@Service
public class AcquisitionFileService implements IAcquisitionFileService {

    @Autowired
    private IAcquisitionFileRepository fileRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository chainRepository;

    @Override
    public long countByChainAndStateIn(AcquisitionProcessingChain chain, List<AcquisitionFileState> fileStates) {
        long total = 0;
        AcquisitionProcessingChain chainFromDb = chainRepository.getOne(chain.getId());
        for (AcquisitionFileInfo fileInfo : chainFromDb.getFileInfos()) {
            total += fileRepository.countByFileInfoAndStateIn(fileInfo, fileStates);
        }
        return total;
    }

    @Override
    public long countByChain(AcquisitionProcessingChain chain) {
        long total = 0;
        AcquisitionProcessingChain chainFromDb = chainRepository.getOne(chain.getId());
        for (AcquisitionFileInfo fileInfo : chainFromDb.getFileInfos()) {
            total += fileRepository.countByFileInfo(fileInfo);
        }
        return total;
    }

    @Override
    public AcquisitionFile save(AcquisitionFile file) {
        return fileRepository.save(file);
    }

    @Override
    public Page<AcquisitionFile> search(String filePath, List<AcquisitionFileState> state, Long productId, Long chainId,
            OffsetDateTime from, Pageable pageable) {
        return fileRepository.findAll(AcquisitionFileSpecifications.search(filePath, state, productId, chainId, from),
                                      pageable);
    }

}
