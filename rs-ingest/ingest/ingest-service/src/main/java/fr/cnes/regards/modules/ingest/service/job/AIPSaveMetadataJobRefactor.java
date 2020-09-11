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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.service.aip.IAIPMetadataServiceRefactor;

/**
 * @author Iliana Ghazali
 */
public class AIPSaveMetadataJobRefactor extends AbstractJob<Void> {

    public static final String SAVE_METADATA_REQUEST = "SAVE_METADATA_REQUEST";

    private AIPSaveMetadataRequestRefactor request;

    @Autowired
    private IAIPMetadataServiceRefactor aipMetadataServiceRefactor;

    @Override
    public boolean needWorkspace() {
        return true;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        this.request = getValue(parameters, SAVE_METADATA_REQUEST, type);
    }

    @Override
    public void run() {
        logger.debug("[AIP SAVE METADATA JOB] Running job for 1 AIPSaveMetaDataRequest request");
        long start = System.currentTimeMillis();
        aipMetadataServiceRefactor.writeZips(request, getWorkspace());
        try {
            aipMetadataServiceRefactor.writeDump(request, getWorkspace());
        } catch (RsRuntimeException e) {
            aipMetadataServiceRefactor.handleError(request, e.getMessage());
            throw e;
        }
        logger.debug("[AIP SAVE META JOB] Job handled for 1 AIPSaveMetaDataRequest request in {}ms",
                     System.currentTimeMillis() - start);

        // there is only one request per job so interruption can be ignored i.e this job(i.e. request) will be fully handled.
    }

}
