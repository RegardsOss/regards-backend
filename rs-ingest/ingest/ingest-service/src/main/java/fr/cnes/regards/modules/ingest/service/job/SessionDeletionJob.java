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
package fr.cnes.regards.modules.ingest.service.job;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.dao.ISessionDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.SessionDeletionRequest;

/**
 * This job handles session deletion requests
 *
 * @author Marc SORDI
 */
public class SessionDeletionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeletionJob.class);

    public static final String ID = "ID";

    @Autowired
    private ISessionDeletionRequestRepository deletionRequestRepository;

    private SessionDeletionRequest deletionRequest;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve deletion request
        Long database_id = getValue(parameters, ID);
        Optional<SessionDeletionRequest> oDeletionRequest = deletionRequestRepository.findById(database_id);

        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", database_id));
        }

        deletionRequest = oDeletionRequest.get();
    }

    @Override
    public void run() {

        // TODO Léo
        // TODO Use deletion request to delete SIP and AIP and files
        // TODO save and publish request ERROR
        // TODO delete and publish request SUCCESS
    }

    @Override
    public int getCompletionCount() {
        // FIXME one step?
        return 1;
    }

}
