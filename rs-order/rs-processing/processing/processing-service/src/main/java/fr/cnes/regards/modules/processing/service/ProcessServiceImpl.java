/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.service.IOutputFileService;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
/**
 * This class is the implementation for the {@link IProcessService} interface.
 *
 * @author gandrieu
 */
@Service
public class ProcessServiceImpl implements IProcessService {

    private final IPProcessRepository processRepo;

    @Autowired
    public ProcessServiceImpl(IPProcessRepository processRepo) {
        this.processRepo = processRepo;
    }

    public Flux<PProcessDTO> findByTenant(String tenant) {
        return processRepo.findAllByTenant(tenant)
                .map(PProcessDTO::fromProcess);
    }

}
