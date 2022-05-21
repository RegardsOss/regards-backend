package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IRequestRetryService {

    /**
     * Retry provided requests and put these requests in CREATED or PENDING
     *
     * @param requests a list of requests in ERROR state
     */
    void relaunchRequests(List<AbstractRequest> requests);
}
