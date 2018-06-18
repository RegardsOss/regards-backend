/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension;

import java.util.List;

import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.OpenSearchParameter;

/**
 * Interface to define a new OpenSearch extension.
 * Extension should define method to : <ul>
 *  <li>Generate {@link OpenSearchParameter}s for the given extension</li>
 *  <li>Provide OpenSearch entity response {@link Module} builder for ATOM+XML format</li>
 *  <li>Generate OpenSearch entity (@link Feature} response in GEO+JSON format</li>
 *  </ul>
 *  {@link Module} builders for ATOM+XML format are provided throught com.rometools.rome module library.
 *  @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 * @author Sébastien Binda
 */
public interface IOpenSearchExtension {

    /**
     * Initialize extension providing opensearch parameters configurations.
     */
    void initialize(List<OpenSearchParameterConfiguration> configurations);

    /**
     * Does the extension actived ?
     * @return {@link boolean}
     */
    boolean isActivated();

    /**
     * Apply extension to the givne search parameters
     * @param parameters TODO
     * @param configurations TODO
     * @return {@link ICriterion}
     */
    ICriterion buildCriterion(List<SearchParameter> parameters);

    /**
     * Create the {@link Module} needed by the rome library to generate the specificity of the extension on each entity of the XML+Atom response.
     * @param entity {@link AbstractEntity} entity to write in XML+Atom format
     * @param gson {@link Gson} tool to serialize objects.
     * @return {@link Module} from rome library
     */
    Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson);

    /**
     * Add parameter into the given {@link Feature} for the {@link AbstractEntity} for Geojson response
     * @param entity {@link AbstractEntity} to write in geojson format.
     * @param feature {@link Feature} from geojson standard
     */
    void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature);

    /**
     * Apply extension for the given {@link OpenSearchParameter} during opensearch xml descriptor build.
     * @param parameter {@link OpenSearchParameter}
     */
    void applyExtensionToDescriptionParameter(OpenSearchParameter parameter, DescriptionParameter descParameter);

    /**
     * Apply extension to the global openSearch description.
     */
    void applyExtensionToDescription(OpenSearchDescription openSearchDescription);

}
