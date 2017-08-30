package fr.cnes.regards.modules.order.domain.basket;

import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * An object used to add a selection on a basket (only used by BasketController)
 * @author oroussel
 */
public class BasketSelectionRequest {

    /**
     * Opensearch request permitting to retrieve data
     */
    private String selectAllOpenSearchRequest;

    /**
     * A set of IP_ID to exclude is openSearchRequest exists OR the set of IP_ID cocnerned by the request
     */
    private Set<String> ipIds;

    public BasketSelectionRequest() {
    }

    public String getSelectAllOpenSearchRequest() {
        return selectAllOpenSearchRequest;
    }

    public void setSelectAllOpenSearchRequest(String openSearchRequest) {
        this.selectAllOpenSearchRequest = Strings.emptyToNull(openSearchRequest);
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
    public String computeOpenSearchRequest() {
        String ipIdsOpenSearch = null;
        // ipIds specified
        if ((ipIds != null) && !ipIds.isEmpty()) {
            ipIdsOpenSearch = "ipId:(" + Joiner.on(" OR ").join(ipIds) + ")";
        } else { // no IpIds specified => selectAll
            return selectAllOpenSearchRequest;
        }
        // No selectAll specified, ipIds are given
        if (selectAllOpenSearchRequest == null) {
            return ipIdsOpenSearch;
        }
        // SelectAll specified, ipIds must be retains
        return "(" + selectAllOpenSearchRequest + ") AND NOT(" + ipIdsOpenSearch + ")";
    }
}
