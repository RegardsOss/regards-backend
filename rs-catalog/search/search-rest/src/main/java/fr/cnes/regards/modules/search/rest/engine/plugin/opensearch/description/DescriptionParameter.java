package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description;

import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;

public class DescriptionParameter {

    private AttributeModel attributeModel;

    private OpenSearchParameterConfiguration configuration;

    private QueryableAttribute queryableAttribute;

    public DescriptionParameter(AttributeModel attributeModel, OpenSearchParameterConfiguration configuration,
            QueryableAttribute queryableAttribute) {
        super();
        this.attributeModel = attributeModel;
        this.configuration = configuration;
        this.queryableAttribute = queryableAttribute;
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

    public QueryableAttribute getQueryableAttribute() {
        return queryableAttribute;
    }

    public void setQueryableAttribute(QueryableAttribute queryableAttribute) {
        this.queryableAttribute = queryableAttribute;
    }

}
