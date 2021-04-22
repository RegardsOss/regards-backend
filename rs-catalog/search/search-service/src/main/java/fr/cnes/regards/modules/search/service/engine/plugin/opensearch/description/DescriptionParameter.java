package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.description;

import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;

public class DescriptionParameter {

    private String name;

    private AttributeModel attributeModel;

    private ParameterConfiguration configuration;

    private QueryableAttribute queryableAttribute;

    public DescriptionParameter(String name, AttributeModel attributeModel, ParameterConfiguration configuration,
            QueryableAttribute queryableAttribute) {
        super();
        this.name = name;
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

    public ParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ParameterConfiguration configuration) {
        this.configuration = configuration;
    }

    public QueryableAttribute getQueryableAttribute() {
        return queryableAttribute;
    }

    public void setQueryableAttribute(QueryableAttribute queryableAttribute) {
        this.queryableAttribute = queryableAttribute;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
