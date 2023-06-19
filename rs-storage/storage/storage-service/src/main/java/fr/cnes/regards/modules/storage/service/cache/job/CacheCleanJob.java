/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.cache.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Cache purge job.<br>
 * Job to delete expired files from cache system.
 *
 * @author Sébastien Binda
 */
public class CacheCleanJob extends AbstractJob<Void> {

    /**
     * Parameter to force deletion of all files in cache and not only expired ones.
     */
    public static final String FORCE_PARAMETER_NAME = "force";

    @Autowired
    private CacheService cacheService;

    private boolean forceMode = false;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        forceMode = parameters.get(FORCE_PARAMETER_NAME).getValue();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        cacheService.purge(forceMode);
        logger.debug("[CACHE CLEAN JOB] Clean files from cache with resetMode={} done in {}ms",
                     forceMode,
                     System.currentTimeMillis() - start);
    }

}
