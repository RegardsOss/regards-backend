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
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Interface to handle {@link AcquisitionFile} entities
 * @author Sébastien Binda
 *
 */
public interface IAcquisitionFileService {

    /**
     * Count number of {@link AcquisitionFile}s associated to the given {@link AcquisitionProcessingChain} and matching
     * the given states.
     * @param chain {@link AcquisitionProcessingChain}
     * @param states {@link AcquisitionFileState}s
     * @return number of matching {@link AcquisitionFile}s
     */
     long countByChainAndStateIn(AcquisitionProcessingChain chain, List<AcquisitionFileState> states);

    /**
     * Count number of {@link AcquisitionFile}s associated to the given {@link AcquisitionProcessingChain}
     * @param chain {@link AcquisitionProcessingChain}
     * @return number of matching {@link AcquisitionFile}s
     */
     long countByChain(AcquisitionProcessingChain chain);

    /**
     * Save or update given {@link AcquisitionFile}
     * @param file {@link AcquisitionFile}
     * @return saved or updated {@link AcquisitionFile}
     */
     AcquisitionFile save(AcquisitionFile file);

    /**
     * Search for {@link AcquisitionFile} entities matching parameters
     * @param filePath {@link String}
     * @param state {@link AcquisitionFileState}
     * @param productId {@link Long} identifier of {@link Product}
     * @param chainId {@link Long} identifier of {@link AcquisitionProcessingChain}
     * @param from {@link OffsetDateTime}
     * @param pageable
     * @return {@link AcquisitionFile}s
     */
     Page<AcquisitionFile> search(String filePath, List<AcquisitionFileState> state, Long productId, Long chainId,
            OffsetDateTime from, Pageable pageable);

}
