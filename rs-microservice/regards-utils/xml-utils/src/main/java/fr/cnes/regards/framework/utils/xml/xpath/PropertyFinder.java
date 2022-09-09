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
package fr.cnes.regards.framework.utils.xml.xpath;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.utils.GeometryConverterUtils;
import fr.cnes.regards.framework.utils.xml.xpath.function.CustomXPathFunctionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Finder into a xml file with XPath.
 *
 * @author Stephane Cortine
 **/
public class PropertyFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFinder.class);

    private final DocumentBuilder builder;

    private final XPath xPath;

    public PropertyFinder() throws ParserConfigurationException {
        // Initialize document builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        builder = factory.newDocumentBuilder();
        // Initialize xPath context
        xPath = XPathFactory.newInstance().newXPath();
        // Manage custom function
        xPath.setXPathFunctionResolver(new CustomXPathFunctionResolver(xPath));
    }

    private NodeList extractByXPath(InputStream is, String xPathValue, boolean lenient)
        throws IOException, SAXException, XPathExpressionException {

        Document document = builder.parse(is);
        xPath.setNamespaceContext(new DynamicNamespaceResolver(document));

        NodeList result = null;
        try {
            result = (NodeList) xPath.compile(xPathValue).evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            LOGGER.debug(String.format("XPath expression error for property %s", xPathValue), e);
            if (!lenient) {
                throw e;
            }
        }
        return result;
    }

    public List<String> extractTextByXPath(InputStream is, String xPathValue, boolean lenient)
        throws IOException, SAXException, XPathExpressionException {

        List<String> result = new ArrayList<>();

        NodeList nodes = extractByXPath(is, xPathValue, lenient);
        if (nodes != null) {
            for (int index = 0; index < nodes.getLength(); index++) {
                result.add(nodes.item(index).getTextContent());
            }
        }
        return result;
    }

    public List<IGeometry> extractGeometryByXPath(InputStream is, String xPathValue, int pointSampling, boolean lenient)
        throws IOException, SAXException, XPathExpressionException {

        List<IGeometry> result = new ArrayList<>();

        NodeList nodes = extractByXPath(is, xPathValue, lenient);
        if (nodes != null) {
            GeometryConverterUtils geometryConverterUtils = new GeometryConverterUtils(pointSampling);
            for (int index = 0; index < nodes.getLength(); index++) {
                IGeometry geometry = geometryConverterUtils.convert(nodes.item(index));
                if (geometry != null) {
                    result.add(geometry);
                }
            }
        }
        return result;
    }
}
