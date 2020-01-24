package fr.cnes.regards.modules.ingest.service.request;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class RequestRetryService implements IRequestRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestRetryService.class);

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Override
    public void relaunchRequests(List<AbstractRequest> requests) {
        // Change requests states
        for (AbstractRequest request : requests) {
            // Requests must be in ERROR state
            if (request.getState() == InternalRequestState.ERROR
                    || request.getState() == InternalRequestState.ABORTED) {
                // Rollback the state to TO_SCHEDULE
                requestService.switchRequestState(request);
            } else {
                LOGGER.error(
                        "Cannot relaunch the request {} because this request is neither in ERROR or ABORTED state. It was in {} state",
                        request.getId(),
                        request.getState());
            }
        }
        requestService.scheduleRequests(requests);

        MultiValueMap<String, IngestRequest> ingestRequestToSchedulePerChain = new LinkedMultiValueMap<>();
        // For macro job, create a job
        for (AbstractRequest request : requests) {
            if ((request.getState() == InternalRequestState.CREATED)) {
                if (requestService.isJobRequest(request)) {
                    requestService.scheduleJob(request);
                } else if (request instanceof IngestRequest) {
                    IngestRequest ingestRequest = (IngestRequest) request;
                    ingestRequestToSchedulePerChain.add(ingestRequest.getMetadata().getIngestChain(), ingestRequest);
                }
            }
        }
        ingestRequestToSchedulePerChain.keySet().forEach(chain -> ingestRequestService
                .scheduleIngestProcessingJobByChain(chain, ingestRequestToSchedulePerChain.get(chain)));
    }

}
