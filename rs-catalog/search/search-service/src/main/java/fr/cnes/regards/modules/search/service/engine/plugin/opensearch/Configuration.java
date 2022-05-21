/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for Opensearch description.xml builder
 *
 * @author Sébastien Binda
 */
@Component
@ConfigurationProperties(prefix = "opensearch")
public class Configuration {

    private String queryParameterValue = "searchTerms";

    private String queryParameterTitle = "Free text search";

    private String developer = "http://www.c-s.fr CS-SI Team";

    private String urlsRel = "results";

    private boolean adultContent = false;

    private String language = "en";

    public Configuration() {
        // Empty constructor needed for Spring
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

    public String getUrlsRel() {
        return urlsRel;
    }

    public void setUrlsRel(String urlsRel) {
        this.urlsRel = urlsRel;
    }

    public boolean isAdultContent() {
        return adultContent;
    }

    public void setAdultContent(boolean adultContent) {
        this.adultContent = adultContent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
