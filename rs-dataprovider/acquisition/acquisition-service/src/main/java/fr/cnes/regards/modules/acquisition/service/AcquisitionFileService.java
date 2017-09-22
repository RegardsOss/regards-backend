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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;

/**
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class AcquisitionFileService implements IAcquisitionFileService {

    private final IAcquisitionFileRepository acqfileRepository;

    public AcquisitionFileService(IAcquisitionFileRepository repository) {
        super();
        this.acqfileRepository = repository;
    }

    @Override
    public AcquisitionFile save(AcquisitionFile acqFile) {
        return acqfileRepository.save(acqFile);
    }

    @Override
    public List<AcquisitionFile> retrieveAll() {
        final List<AcquisitionFile> acqFiles = new ArrayList<>();
        acqfileRepository.findAll().forEach(c -> acqFiles.add(c));
        return acqFiles;
    }

    @Override
    public AcquisitionFile retrieve(Long id) {
        return acqfileRepository.findOne(id);
    }

    @Override
    public void delete(Long id) {
        acqfileRepository.delete(id);
    }

}
