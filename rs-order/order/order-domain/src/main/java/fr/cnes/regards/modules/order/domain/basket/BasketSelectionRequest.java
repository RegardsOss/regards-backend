package fr.cnes.regards.modules.order.domain.basket;

import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import fr.cnes.regards.modules.order.domain.exception.BadBasketSelectionRequest;

/**
 * An object used to add a selection on a basket (only used by BasketController).
 * selectAllOpenSearchRequest : <br>
 * - null -> ipIds must be not null nor not empty. The request corresponds to the given IP_IDs
 * - "" -> ALL minus given IP_IDs (if provided)
 * - else given request minus given IP_IDS
 * @author oroussel
 */
public class BasketSelectionRequest {

    /**
     * Opensearch request permitting to retrieve data or null if only IP_IDs must be retrieved
     */
    private String selectAllOpenSearchRequest;

    /**
     * A set of IP_ID to exclude if openSearchRequest exists OR the set of IP_ID concerned by the request
     */
    private Set<String> ipIds;

    public BasketSelectionRequest() {
    }

    public String getSelectAllOpenSearchRequest() {
        return selectAllOpenSearchRequest;
    }

    public void setSelectAllOpenSearchRequest(String openSearchRequest) {
        this.selectAllOpenSearchRequest = openSearchRequest;
    }

    public Set<String> getIpIds() {
        return ipIds;
    }

    public void setIpIds(Set<String> ipIds) {
        this.ipIds = ipIds;
    }

    /**
     * Compute openSearch request taking into account all parameters (selectAllOpenSearchRequest, ipIds, ...)
     */
    public String computeOpenSearchRequest() throws BadBasketSelectionRequest {
        String ipIdsOpenSearch = null;
        // ipIds specified
        if ((ipIds != null) && !ipIds.isEmpty()) {
            ipIdsOpenSearch = "ipId:(" + Joiner.on(" OR ").join(ipIds) + ")";
        } else if (selectAllOpenSearchRequest == null) {
            throw new BadBasketSelectionRequest("If opensearch request is null, at least on IP_ID must be provided");
        } else { // no IpIds specified => selectAll
            return selectAllOpenSearchRequest;
        }
        // No selectAll specified, ipIds are given
        if (selectAllOpenSearchRequest == null) {
            return ipIdsOpenSearch;
        }
        // SelectAll specified, ipIds must be retains
        // BEWARE OF empty OpenSearch request (means "ALL")
        if (selectAllOpenSearchRequest.isEmpty()) {
            return "NOT(" + ipIdsOpenSearch + ")";
        }
        return "(" + selectAllOpenSearchRequest + ") AND NOT(" + ipIdsOpenSearch + ")";
    }
}
