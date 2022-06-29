/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.job;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.plugin.IPeriodicActionProgressManager;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class PeriodicActionProgressManager implements IPeriodicActionProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicActionProgressManager.class);

    private final FileReferenceService fileRefService;

    private final Set<String> pendingActionSucceedUrls = Sets.newHashSet();

    public PeriodicActionProgressManager(FileReferenceService fileRefService) {
        this.fileRefService = fileRefService;
    }

    protected void bulkSavePendings() {
        fileRefService.handleRemainingPendingActionSuccess(pendingActionSucceedUrls);
        pendingActionSucceedUrls.clear();
    }

    @Override
    public void storagePendingActionSucceed(String pendingActionSucceedUrl) {
        LOG.info("[STORE PENDING ACTION SUCCEED] Stored file {}, remaining pending action is over with success.",
                 pendingActionSucceedUrl);
        pendingActionSucceedUrls.add(pendingActionSucceedUrl);
        if (pendingActionSucceedUrls.size() > 10) {
            bulkSavePendings();
        }
        // Do not advance job completion as this is an asynchronous action not associated to job store requests.
    }

}
