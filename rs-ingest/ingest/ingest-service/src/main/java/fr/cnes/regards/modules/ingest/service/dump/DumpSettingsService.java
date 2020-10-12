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


package fr.cnes.regards.modules.ingest.service.dump;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IDumpSettingsRepository;
import fr.cnes.regards.modules.ingest.domain.dump.DumpSettings;

/**
 * {@link IDumpSettingsService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class DumpSettingsService implements IDumpSettingsService {

    @Autowired
    private IDumpSettingsRepository dumpSettingsRepository;


    @Override
    public DumpSettings retrieve() {
        Optional<DumpSettings> dumpSettingsOpt = dumpSettingsRepository.findFirstBy();
        DumpSettings dumpSettings;
        if (!dumpSettingsOpt.isPresent()) {
            // init new settings with default parameters
            dumpSettings = new DumpSettings(true, "0 0 0 1-7 * SUN", null, null);
            dumpSettings = dumpSettingsRepository.save(dumpSettings);
        } else {
            // get existing settings
            dumpSettings = dumpSettingsOpt.get();
        }
        return dumpSettings;
    }

    @Override
    public DumpSettings update(DumpSettings dumpSettings) throws
            EntityNotFoundException {
        if (!dumpSettingsRepository.existsById(dumpSettings.getId())) {
            throw new EntityNotFoundException(dumpSettings.getId().toString(), DumpSettings.class);
        } return dumpSettingsRepository.save(dumpSettings);
    }

}
