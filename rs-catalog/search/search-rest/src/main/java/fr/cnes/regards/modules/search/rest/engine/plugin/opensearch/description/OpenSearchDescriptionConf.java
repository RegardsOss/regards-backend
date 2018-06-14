package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchDescriptionConf {

    private String queryParameterName = "q";

    private String queryParameterValue = "searchTerms";

    private String queryParameterTitle = "Free text search";

    private String developer = "http://www.c-s.fr CS-SI Team";

    private String attribution = "http://www.cnes.fr CNES Centre National d'Etudes Spatiales - Copyright 2017-2018, All Rigts reserved";

    public OpenSearchDescriptionConf() {
    }

    public String getQueryParameterName() {
        return queryParameterName;
    }

    public void setQueryParameterName(String queryParameterName) {
        this.queryParameterName = queryParameterName;
    }

    public String getQueryParameterValue() {
        return queryParameterValue;
    }

    public void setQueryParameterValue(String queryParameterValue) {
        this.queryParameterValue = queryParameterValue;
    }

    public String getQueryParameterTitle() {
        return queryParameterTitle;
    }

    public void setQueryParameterTitle(String queryParameterTitle) {
        this.queryParameterTitle = queryParameterTitle;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

}
