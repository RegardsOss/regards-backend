package fr.cnes.regards.modules.ingest.service.job;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IngestRequestSpecifications;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ChooseVersioningJob extends AbstractJob<Void> {

    public static final String CRITERIA_JOB_PARAM_NAME = "CRITERIA_JOB_PARAM_NAME";

    private ChooseVersioningRequestParameters filters;

    @Value("${regards.request.versioning.iteration-limit:1000}")
    private Integer requestIterationLimit;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Override
    public void run() {
        filters.setStates(Sets.newHashSet(InternalRequestState.WAITING_VERSIONING_MODE));
        Pageable pageable = PageRequest.of(0, requestIterationLimit);
        Page<IngestRequest> requestPage = ingestRequestRepository
                .findAll(IngestRequestSpecifications.searchAllByFilters(filters, pageable), pageable);
        do {
            ingestRequestService.fromWaitingTo(requestPage.getContent(), filters.getNewVersioningMode());
            pageable = requestPage.getPageable().next();
            requestPage = ingestRequestRepository
                    .findAll(IngestRequestSpecifications.searchAllByFilters(filters, pageable), pageable);
        }
        while(requestPage.hasNext());
    }

    @Override
    public boolean needWorkspace() {
        return false;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.filters = getValue(parameters, CRITERIA_JOB_PARAM_NAME, ChooseVersioningRequestParameters.class);
    }

    @Override
    public int getCompletionCount() {
        return 0;
    }
}
