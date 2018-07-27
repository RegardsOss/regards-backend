package fr.cnes.regards.modules.search.domain;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.util.MultiValueMap;

public class ComplexSearchRequest {

    /**
     * Engine to use for the research
     */
    private final String engineType;

    /**
     * Dataset urn identifier on which search must be applyed. If null search is applyed on the whole catalog
     */
    private final String datasetUrn;

    /**
     * Search engine query parameters
     */
    private final MultiValueMap<String, String> searchParameters;

    /**
     * Additional entity ids to return with the search results.
     */
    private final Collection<String> entityIdsToInclude;

    /**
     * Entity ids to exclud from search results.
     */
    private final Collection<String> entityIdsToExclude;

    /**
     * Maximum creation date of researched entities. If null no date criterion is added to the search.
     */
    private final OffsetDateTime searchDateLimit;

    public ComplexSearchRequest(String engineType, String datasetUrn, MultiValueMap<String, String> searchParameters,
            Collection<String> entityIdsToInclude, Collection<String> entityIdsToExclude,
            OffsetDateTime searchDateLimit) {
        super();
        this.engineType = engineType;
        this.datasetUrn = datasetUrn;
        this.searchParameters = searchParameters;
        this.entityIdsToInclude = entityIdsToInclude;
        this.entityIdsToExclude = entityIdsToExclude;
        this.searchDateLimit = searchDateLimit;
    }

    public String getEngineType() {
        return engineType;
    }

    public String getDatasetUrn() {
        return datasetUrn;
    }

    public MultiValueMap<String, String> getSearchParameters() {
        return searchParameters;
    }

    public OffsetDateTime getSearchDateLimit() {
        return searchDateLimit;
    }

    public Collection<String> getEntityIdsToExclude() {
        return entityIdsToExclude;
    }

    public Collection<String> getEntityIdsToInclude() {
        return entityIdsToInclude;
    }

}
