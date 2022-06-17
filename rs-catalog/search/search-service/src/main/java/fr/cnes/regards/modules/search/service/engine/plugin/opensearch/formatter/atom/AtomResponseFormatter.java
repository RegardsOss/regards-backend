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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom;

import com.google.gson.Gson;
import com.rometools.modules.opensearch.entity.OSQuery;
import com.rometools.modules.opensearch.impl.OpenSearchModuleImpl;
import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.feed.synd.SyndPersonImpl;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.OpenSearchMediaType;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.AbstractResponseFormatter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.DataFileHrefBuilder;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.MediaType;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Build open search responses in ATOM format through rome library handling :
 * <ul>
 * <li>Opensearch parameters extension</li>
 * <li>{@link IOpenSearchExtension}s for additional extensions like geo+time or media.</li>
 * </ul>
 *
 * @author Sébastien Binda
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 */
public class AtomResponseFormatter extends AbstractResponseFormatter<Entry, Feed> {

    /**
     * ATOM format version
     */
    public static final String ATOM_VERSION = "atom_1.0";

    protected final Gson gson;

    public AtomResponseFormatter(Gson gson, String token) {
        super(token);
        this.gson = gson;
    }

    @Override
    public void clear() {
        response.getEntries().clear();
    }

    @Override
    public Feed build() {
        return this.response;
    }

    @Override
    protected Feed buildResponse() {
        Feed feed = new Feed(ATOM_VERSION);
        feed.getModules().add(new OpenSearchModuleImpl());
        return feed;
    }

    @Override
    protected Entry buildFeature() {
        return new Entry();
    }

    @Override
    protected void addResponseLanguage(String language) {
        response.setLanguage(language);
    }

    @Override
    protected void addResponseUpdated() {
        response.setUpdated(Date.valueOf(LocalDate.now()));
    }

    @Override
    protected void addResponseAuthor(String contact, String attribution) {
        SyndPerson author = new SyndPersonImpl();
        author.setEmail(contact);
        author.setName(attribution);
        response.getAuthors().add(author);
    }

    @Override
    protected void addResponseQuery(SearchContext context, String role) {
        OpenSearchModuleImpl osm = getResponseOpenSearchModule();

        // Add the query from opensearch module
        OSQuery query = new OSQuery();
        if ((context.getQueryParams() != null) && context.getQueryParams().containsKey("q")) {
            query.setSearchTerms(context.getQueryParams().get("q").get(0));
        }
        query.setStartPage(context.getPageable().getPageNumber());
        query.setRole(role);
        osm.addQuery(query);
    }

    @Override
    protected void addResponseOpenSearchDescription(String openSearchDescriptionUrl) {
        OpenSearchModuleImpl osm = getResponseOpenSearchModule();

        // Add opensearch description link
        Link osDescLink = new Link();
        osDescLink.setHref(openSearchDescriptionUrl);
        osDescLink.setType(OpenSearchMediaType.APPLICATION_OPENSEARCH_DESC_VALUE);
        osm.setLink(osDescLink);

        // Add simple link to opensearch description
        response.getAlternateLinks().add(osDescLink);
    }

    @Override
    protected void addResponseLinks(List<org.springframework.hateoas.Link> links) {
        // do nothing
    }

    @Override
    protected void addResponsePaginationInfos(long totalResults, long startIndex, int itemsPerPage) {
        OpenSearchModuleImpl osm = getResponseOpenSearchModule();
        osm.setItemsPerPage(itemsPerPage);
        osm.setStartIndex((int) startIndex + 1);
        osm.setTotalResults((int) totalResults);
    }

    private OpenSearchModuleImpl getResponseOpenSearchModule() {
        return (OpenSearchModuleImpl) this.response.getModules().get(0);
    }

    @Override
    protected void addResponseDescription(String description) {
        Content content = new Content();
        content.setType(Content.TEXT);
        content.setValue(description);
        response.setSubtitle(content);
    }

    @Override
    protected void addResponseTitle(String title) {
        response.setTitle(title);
    }

    @Override
    protected void addResponseId(String searchId) {
        response.setId(searchId);
    }

    @Override
    protected void addFeatureProviderId(String providerId) {
        // do nothing
    }

    @Override
    protected void addFeatureTitle(String title) {
        this.feature.setTitle(title);
    }

    @Override
    protected void addFeatureId(UniformResourceName id) {
        this.feature.setId(id.toString());
    }

    @Override
    protected void addToResponse(Entry entity) {
        response.getEntries().add(entity);
    }

    @Override
    protected void addFeatureLinks(List<org.springframework.hateoas.Link> entityLinks) {
        entityLinks.forEach(link -> {
            Link feedEntityLink = new Link();
            feedEntityLink.setHref(link.getHref());
            feedEntityLink.setType(MediaType.APPLICATION_ATOM_XML_VALUE);
            if (link.getRel().equals(IanaLinkRelations.SELF)) {
                feedEntityLink.setTitle(String.format("ATOM link for %s", feature.getId()));
            }
            this.feature.getAlternateLinks().add(feedEntityLink);
        });
    }

    @Override
    protected void addFeatureServices(DataFile firstRawData) {
        Link rawdataLink = new Link();
        rawdataLink.setHref(DataFileHrefBuilder.getDataFileHref(firstRawData, scope));
        rawdataLink.setType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        rawdataLink.setTitle(String.format("Download rawdata for product %s", feature.getId()));
        this.feature.getAlternateLinks().add(rawdataLink);
    }

    @Override
    protected void updateEntityWithExtension(IOpenSearchExtension extension,
                                             AbstractEntity<EntityFeature> entity,
                                             List<ParameterConfiguration> paramConfigurations) {
        extension.formatAtomResponseEntry(entity, paramConfigurations, this.feature, gson, scope);
    }

}
