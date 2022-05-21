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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.impl;

import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.io.ModuleGenerator;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.RegardsModule;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module generator to handle specific REGARDS models opensearch parameters into ATOM format responses.
 * com.rometools.rome module generator to handle specifics regards model attributes.
 * This ModuleGenerator is executed by rome (see rome.properties)
 *
 * @author Sébastien Binda
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 */
public class RegardsModuleGenerator implements ModuleGenerator {

    /**
     * REGARDS namespace for opensearch parameters
     */
    public static final Namespace REGARDS_NS = Namespace.getNamespace(RegardsExtension.REGARDS_NS, RegardsModule.URI);

    @Override
    public String getNamespaceUri() {
        return RegardsModule.URI;
    }

    private static final Set<Namespace> NAMESPACES;

    private static final String LABEL = "label";

    private static final String IPID = "ipId";

    private static final String SIPID = "sipId";

    private static final String TAGS = "tags";

    private static final String TYPE = "type";

    static {
        final Set<Namespace> nss = new HashSet<Namespace>();
        nss.add(REGARDS_NS);
        NAMESPACES = Collections.unmodifiableSet(nss);
    }

    /**
     * Returns a set with all the URIs (JDOM Namespace elements) this module generator uses.
     * <p/>
     * It is used by the the feed generators to add their namespace definition in the root element
     * of the generated document (forward-missing of Java 5.0 Generics).
     * <p/>
     *
     * @return a set with all the URIs (JDOM Namespace elements) this module generator uses.
     */
    @Override
    public Set<Namespace> getNamespaces() {
        return NAMESPACES;
    }

    @Override
    public void generate(final Module module, final Element element) {
        final RegardsModule regardsModule = (RegardsModule) module;

        // Add standard attributs
        // Geometry is not added here. This standard attribute should be handled by the Time&Geo extension
        EntityFeature entity = regardsModule.getEntity();
        addStandardElement(element, LABEL, entity.getLabel(), regardsModule.getGsonBuilder());
        addStandardElement(element, IPID, entity.getId(), regardsModule.getGsonBuilder());
        addStandardElement(element, SIPID, entity.getProviderId(), regardsModule.getGsonBuilder());
        addStandardElement(element, TAGS, entity.getTags(), regardsModule.getGsonBuilder());
        addStandardElement(element, TYPE, entity.getType(), regardsModule.getGsonBuilder());
        regardsModule.getEntity()
                     .getProperties()
                     .stream()
                     .forEach(property -> element.addContent(generateAttributeElement(property,
                                                                                      regardsModule.getGsonBuilder())));

    }

    protected Element generateAttributeElement(IProperty<?> attribute, Gson gson) {
        Element elt;
        if (attribute instanceof ObjectProperty) {
            elt = new Element(attribute.getName(), REGARDS_NS);
            elt.addContent(((ObjectProperty) attribute).getValue()
                                                       .stream()
                                                       .map(a -> this.generateAttributeElement(a, gson))
                                                       .collect(Collectors.toList()));
        } else {
            elt = generateElement(attribute.getName(), attribute.getValue(), gson);
        }
        return elt;
    }

    protected Element generateElement(String name, Object value, Gson gson) {
        Element element = new Element(name, REGARDS_NS);
        element.addContent(gson.toJson(value));
        return element;
    }

    protected void addStandardElement(Element rootElement, String name, Object value, Gson gson) {
        if ((rootElement != null) && (value != null)) {
            rootElement.addContent(generateElement(name, value, gson));
        }
    }

}
