/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.xml.xpath;

import fr.cnes.regards.framework.utils.xml.xpath.function.CustomXPathFunctionResolver;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Dynamic namespace resolver.
 *
 * @author Marc SORDI, Stephane CORTINE
 */
public class DynamicNamespaceResolver implements NamespaceContext {

    /**
     * Key/value pairs representation of prefix/URI
     */
    private final Map<String, String> xmlns = new HashMap<>();

    /**
     * Key/value pairs representation of URI/prefix
     */
    private final Map<String, String> reverseXmlns = new HashMap<>();

    private final Document document;

    public DynamicNamespaceResolver(Document document) {
        this.document = document;

        xmlns.put(CustomXPathFunctionResolver.EXTENDED_FUNCTION_PREFIX,
                  CustomXPathFunctionResolver.EXTENDED_FUNCTION_URI);
        xmlns.forEach((prefix, uri) -> reverseXmlns.put(uri, prefix));
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            return document.lookupNamespaceURI(null);
        } else {
            String namespaceURI = document.lookupNamespaceURI(prefix);
            if (namespaceURI != null) {
                return namespaceURI;
            } else {
                return xmlns.get(prefix);
            }
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        // Not use at the moment
        String prefix = document.lookupPrefix(namespaceURI);
        if (prefix != null) {
            return prefix;
        } else {
            return reverseXmlns.get(namespaceURI);
        }
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

}
