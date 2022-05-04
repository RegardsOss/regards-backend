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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.rometools.modules.georss.GMLGenerator;
import com.rometools.modules.georss.GMLModuleImpl;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.io.ModuleGenerator;

import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml.GmlTimeModule;

/**
 * Module generator to handle TIME & GEO opensearch parameters into ATOM format responses.
 * This ModuleGenerator is executed by rome (see rome.properties)
 *
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 * @author Sébastien Binda
 */
public class GmlTimeModuleGenerator extends GMLGenerator implements ModuleGenerator {

    @Override
    public String getNamespaceUri() {
        return GmlTimeModule.URI;
    }

    private static final Set<Namespace> NAMESPACES;

    private static final String VALID_TIME = "ValidTime";

    private static final String TIME_PERIOD = "TimePeriod";

    private static final String TIME_START = "beginPosition";

    private static final String TIME_STOP = "endPosition";

    static {
        final Set<Namespace> nss = new HashSet<Namespace>();
        nss.add(Namespace.getNamespace("gml", GeoRSSModule.GEORSS_GML_URI));
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
        final GmlTimeModuleImpl gmlMod = (GmlTimeModuleImpl) module;

        // Create root element
        if ((gmlMod.getStartDate() != null) && (gmlMod.getStopDate() != null)) {
            Element rootElement = new Element(VALID_TIME, GMLModuleImpl.GML_NS);
            Element timePeriod = new Element(TIME_PERIOD, GMLModuleImpl.GML_NS);
            Element startDate = new Element(TIME_START, GMLModuleImpl.GML_NS);
            startDate.addContent(gmlMod.getGsonBuilder().toJson(gmlMod.getStartDate()));
            Element stopDate = new Element(TIME_STOP, GMLModuleImpl.GML_NS);
            stopDate.addContent(gmlMod.getGsonBuilder().toJson(gmlMod.getStopDate()));
            timePeriod.addContent(startDate);
            timePeriod.addContent(stopDate);
            rootElement.addContent(timePeriod);
            element.addContent(rootElement);
        }
        if (gmlMod.getGeometry() != null) {
            super.generate(module, element);
        }
    }

}
