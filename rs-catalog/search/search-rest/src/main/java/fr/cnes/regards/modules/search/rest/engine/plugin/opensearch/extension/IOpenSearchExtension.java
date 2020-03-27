/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.ExtensionException;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;

/**
 * Interface to define a new OpenSearch extension.
 * Extension should define method to : <ul>
 *  <li>Handle activation of the extension.</li>
 *  <li>Generate {@link OpenSearchParameter}s for the given extension. Used to write the Opensearch descriptor xml file.</li>
 *  <li>Generate OpenSearch entity {@link Entry} builder for ATOM+XML format. Used to format Opensearch search responses in ATOM.</li>
 *  <li>Generate OpenSearch entity (@link Feature} response in GEO+JSON format. Used to format Opensearch search responses in GEOJson.</li>
 *  </ul>
 * @author Sébastien Binda
 */
public interface IOpenSearchExtension {

    /**
     * Does the extension actived ?
     * @return {@link boolean}
     */
    boolean isActivated();

    /**
     * Creates the {@link ICriterion} for the current extension from the given {@link SearchParameter}s.
     * Used to run an opensearch search for the current extension.
     * @param parameters {@link SearchParameter}s to build criterion from
     * @return {@link ICriterion}
     */
    ICriterion buildCriterion(List<SearchParameter> parameters) throws ExtensionException;

    /**
     * Create the {@link Module} needed by the rome library to generate the specificity of the extension on each entity of the XML+Atom response.
     * Used to format opensearch response into ATOM+XML format for the current extension.
     * @param entity {@link EntityFeature} entity to write in XML+Atom format
     * @param paramConfigurations {@link ParameterConfiguration} opensearch parameters configurations.
     * @param entry {@link Entry} ATOM feed entry in which add the extension format
     * @param gson {@link Gson} tool to serialize objects.
     */
    void formatAtomResponseEntry(EntityFeature entity, List<ParameterConfiguration> paramConfigurations, Entry entry,
            Gson gson, String token);

    /**
     * Add parameter into the given {@link Feature} for the {@link EntityFeature} for Geojson response
     * @param entity {@link EntityFeature} to write in geojson format.
     * @param paramConfigurations {@link ParameterConfiguration} opensearch parameters configurations.
     * @param feature {@link Feature} from geojson standard
     */
    void formatGeoJsonResponseFeature(EntityFeature entity, List<ParameterConfiguration> paramConfigurations,
            Feature feature, String token);

    /**
     * Apply extension for the given {@link OpenSearchParameter} during opensearch xml descriptor build.
     * @param parameter {@link OpenSearchParameter}
     */
    void applyToDescriptionParameter(OpenSearchParameter parameter, DescriptionParameter descParameter);

    /**
     * Apply extension to the global openSearch description.
     */
    void applyToDescription(OpenSearchDescription openSearchDescription);

    /**
     * Generates new parameters handled exlusivly by the current extesion.
     * @return {@link OpenSearchParameter}s
     */
    List<OpenSearchParameter> addParametersToDescription();

}
