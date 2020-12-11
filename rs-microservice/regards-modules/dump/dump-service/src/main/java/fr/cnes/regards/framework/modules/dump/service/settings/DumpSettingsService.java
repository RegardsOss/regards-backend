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


package fr.cnes.regards.framework.modules.dump.service.settings;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.dump.dao.IDumpSettingsRepository;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;

/**
 * see {@link IDumpSettingsService}
 * @author Iliana Ghazali
 */

@Service
@RegardsTransactional
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
    public boolean update(DumpSettings dumpSettings)  {
        boolean isUpdated = false;
        Optional<DumpSettings> dumpSettingsOpt = dumpSettingsRepository.findById(dumpSettings.getId());
        if (!dumpSettingsOpt.isPresent() || !dumpSettingsOpt.get().equals(dumpSettings)) {
            isUpdated = true;
            dumpSettingsRepository.save(dumpSettings);
        }
        return isUpdated;
    }

    @Override
    public void resetLastDumpDate() {
        // reset last dump date to null if already present
        Optional<DumpSettings> oLastDump = dumpSettingsRepository.findFirstBy();
        if (oLastDump.isPresent()) {
            DumpSettings lastDump = oLastDump.get();
            lastDump.setLastDumpReqDate(null);
            dumpSettingsRepository.save(lastDump);
        }
        // if there is no dump setting it means the last dump date is already null, there is nothing to do
    }

    @Override
    public void resetSettings() {
        // delete old settings and init new ones (by default)
        dumpSettingsRepository.deleteById(DumpSettings.DUMP_CONF_ID);
        this.retrieve();
    }
}
