package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension;

import java.util.List;

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;

public class SearchParameter {

    private AttributeModel attributeModel;

    private OpenSearchParameterConfiguration configuration;

    private List<String> searchValues;

    public SearchParameter(AttributeModel attributeModel, OpenSearchParameterConfiguration configuration,
            List<String> searchValues) {
        super();
        this.attributeModel = attributeModel;
        this.configuration = configuration;
        this.searchValues = searchValues;
    }

    public AttributeModel getAttributeModel() {
        return attributeModel;
    }

    public void setAttributeModel(AttributeModel attributeModel) {
        this.attributeModel = attributeModel;
    }

    public OpenSearchParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(OpenSearchParameterConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<String> getSearchValues() {
        return searchValues;
    }

    public void setSearchValues(List<String> searchValues) {
        this.searchValues = searchValues;
    }

}
