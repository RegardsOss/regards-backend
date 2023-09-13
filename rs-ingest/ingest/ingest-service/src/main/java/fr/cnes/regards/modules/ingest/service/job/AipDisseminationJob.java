/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * TODO Doc in next odin task
 *
 * @author Thomas GUILLOU
 **/
public class AipDisseminationJob extends AbstractJob<Void> {

    public static final String AIP_DISSEMINATION_REQUEST_IDS = "AIP_DISSEMINATION_REQUEST_IDS";

    @Autowired
    private AipDisseminationService aipDisseminationService;

    private List<AipDisseminationRequest> disseminationRequests;

    @Autowired
    private IAipDisseminationRequestRepository disseminationRequestRepository;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> aipRequestIds = getValue(parameters, AIP_DISSEMINATION_REQUEST_IDS, type);
        // Retrieve list of AIP save metadata requests to handle
        disseminationRequests = aipDisseminationService.searchRequests(aipRequestIds);
    }

    @Override
    public void run() {
        logger.info("start aipDisseminationJob for {} aips", disseminationRequests.size());
        // TODO to implement
    }

}
