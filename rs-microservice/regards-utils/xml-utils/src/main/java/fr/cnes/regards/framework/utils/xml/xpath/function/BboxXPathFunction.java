/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.xml.xpath.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunctionException;
import java.util.List;

/**
 * @author Stephane Cortine
 **/
public class BboxXPathFunction implements NamedXPathFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(BboxXPathFunction.class);

    private final XPath xPath;

    public BboxXPathFunction(XPath xPath) {
        this.xPath = xPath;
    }

    @Override
    public String getFunctionName() {
        return "bbox";
    }

    /**
     * <gmd:EX_GeographicBoundingBox>
     * <gmd:westBoundLongitude>
     * <gco:Decimal>-129.13090393992763</gco:Decimal>
     * </gmd:westBoundLongitude>
     * <gmd:eastBoundLongitude>
     * <gco:Decimal>-128.46876391015957</gco:Decimal>
     * </gmd:eastBoundLongitude>
     * <gmd:southBoundLatitude>
     * <gco:Decimal>5.266971710251425</gco:Decimal>
     * </gmd:southBoundLatitude>
     * <gmd:northBoundLatitude>
     * <gco:Decimal>5.6494921501577515</gco:Decimal>
     * </gmd:northBoundLatitude>
     * </gmd:EX_GeographicBoundingBox>
     * </gmd:geographicElement>
     * <p>
     * [-129.13090393992763, 5.266971710251425, -128.46876391015957, 5.6494921501577515]
     * [gmd:westBoundLongitude, gmd:southBoundLatitude, gmd:eastBoundLongitude, gmd:northBoundLatitude]
     */
    @Override
    public Object evaluate(@SuppressWarnings("rawtypes") List args) throws XPathFunctionException {
        if (args.size() == 1) {
            NodeList nodes = (NodeList) args.get(0);
            for (int index = 0; index < nodes.getLength(); index++) {
                Node node = nodes.item(index);
                try {
                    String westBoundLongitude = xPath.evaluate("gmd:westBoundLongitude/gco:Decimal", node);
                    String eastBoundLongitude = xPath.evaluate("gmd:eastBoundLongitude/gco:Decimal", node);
                    String southBoundLatitude = xPath.evaluate("gmd:southBoundLatitude/gco:Decimal", node);
                    String northBoundLatitude = xPath.evaluate("gmd:northBoundLatitude/gco:Decimal", node);
                    node.setTextContent("["
                                        + westBoundLongitude
                                        + ","
                                        + southBoundLatitude
                                        + ","
                                        + eastBoundLongitude
                                        + ","
                                        + northBoundLatitude
                                        + "]");
                } catch (XPathExpressionException e) {
                    LOGGER.error(String.format("Failure in function %s", getFunctionName()), e);
                }
            }
            return nodes;
        }
        return null;
    }

}
