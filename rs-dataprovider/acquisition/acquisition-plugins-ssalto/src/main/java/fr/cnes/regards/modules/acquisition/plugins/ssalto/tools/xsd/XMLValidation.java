/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.xsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import org.apache.xerces.parsers.CachingParserPool;
import org.apache.xerces.parsers.StandardParserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Cette classe permet de valider le message XML vis a vis d'un schema XML avant envoi au service SIPAD-NG.
 *
 * Elle s'appuie sur la classe <code>XsdResolver</code> pour acceder aux fichiers schema xsd.
 *
 * @author Christophe Mertz
 */
public class XMLValidation extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLValidation.class);

    /**
     * Le schema permettant de valider le document XML
     */
    protected String xsdFile = null;

    /**
     * Le gestionnaire d'erreur
     */
    private XMLErrorManager xmlErrorManager = null;

    /**
     * Le parser permettant d'analyser le document XML et de le valider vis a vis d'un Schema XML.
     */
    private XMLReader parser = null;

    /**
     * Constructeur - initialise le parser et fixe le mode de validation --> XML Schema
     *
     * @param xmlSchema
     *            L'URL qui pointe vers le fichier XML Schema de validation
     * @param resolver
     *            l'objet permettant de resoudre l'emplacement physique des XML Schema
     * @throws XMLValidationException
     *             Quand une erreur d'initialisation du validateur est levee
     */
    protected XMLValidation(String xmlSchema, EntityResolver resolver, boolean isValidationLoggerEnabled)
            throws XMLValidationException {

        xsdFile = xmlSchema;
        xmlErrorManager = new XMLErrorManager(isValidationLoggerEnabled);

        try {
            // This object allows to keep a grammer pool with the different grammers
            StandardParserConfiguration parserConfig = new StandardParserConfiguration();

            // set validating to true

            // Create an empty grammar pool
            CachingParserPool grammarPool = new CachingParserPool();

            // Connect the two
            parserConfig.setProperty("http://apache.org/xml/properties/internal/grammar-pool", grammarPool);

            parser = grammarPool.createSAXParser();
            parser.setErrorHandler(xmlErrorManager);
            parser.setEntityResolver(resolver);

            // set validation to TRUE
            parser.setFeature("http://xml.org/sax/features/validation", true);

            // indicate that validation should be done using XML Schema file
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

            // Set the namespace schema location if there is a specified schema
            //
            if (xsdFile != null) {
                setNamespaceSchemaLocation(parser, xsdFile);
            }

            parser.setContentHandler(this);
        }

        catch (Exception e) {
            LOGGER.error("Error while initializing the validation parser", e);
            throw new XMLValidationException("Error while initializing the validation parser", e);
        }
    }

    /**
     * Cette methode permet de valider un document XML vis a vis d'un schema XSD
     *
     * @param fileToValidate
     *            le fichier xml a valider
     * @throws XMLValidationException
     *             quand une erreur d'analyse du fichier XML est decouverte
     */
    public void validate(File fileToValidate) throws XMLValidationException {
        try (FileReader reader = new FileReader(fileToValidate)) {
            // Parse the request
            try (BufferedReader br = new BufferedReader(reader)) {
                parser.parse(new InputSource(br));
                // Did we have any errors ?
                if ((!xmlErrorManager.getErrors().isEmpty()) || (!xmlErrorManager.getFatalErrors().isEmpty())) {
                    int errorCount = xmlErrorManager.getErrors().size() + xmlErrorManager.getFatalErrors().size();

                    // Try to make a nice log trace. Too much info ruins the info.
                    // First log the document that caused the error.
                    LOGGER.error(String.format("%s error(s) while parsing document:\n%s", errorCount,
                                               fileToValidate.getAbsoluteFile().getName()));

                    // Log the xml schema used for validation
                    LOGGER.error(String.format("The document is not valid with regards to the XML Schema '%s'",
                                               xsdFile));

                    throw new XMLValidationException(
                            String.format("Error(s) while parsing document:\n%s", fileToValidate.getAbsoluteFile().getName()));
                }
            }
        } catch (XMLValidationException e) {
            // Just throw away, we have already logged all there is to log
            throw e;
        } catch (Exception e) {

            // Print original exception
            // and log the xml schema used for validation
            LOGGER.error(String.format("Error(s) while parsing document:\n{0}", xsdFile), e);

            // Finally, pass on a new exception
            throw new XMLValidationException(
                    String.format("Error(s) while parsing document:\n%s", fileToValidate.getAbsoluteFile().getName()));
        }
    }

    /**
     * Cette methode permet de valider un document XML vis a vis d'un schema XSD
     *
     * @param request
     *            La requete a valider.
     * @throws XMLValidationException
     *             quand une erreur d'analyse du fichier XML est decouverte
     * @since 1.0
     */
    public void validate(String request) throws XMLValidationException {
        try {
            // Parse the request
            parser.parse(new InputSource(new StringReader(request)));

            // Did we have any errors ?
            if ((!xmlErrorManager.getErrors().isEmpty()) || (!xmlErrorManager.getFatalErrors().isEmpty())) {
                int errorCount = xmlErrorManager.getErrors().size() + xmlErrorManager.getFatalErrors().size();

                // Try to make a nice log trace. Too much info ruins the info.
                // First log the document that caused the error.
                LOGGER.error(String.format("%d error(s) while parsing document:\n%s", errorCount, request));

                // Log the xml schema used for validation
                LOGGER.error(String.format("The document is not valid with regards to the XML Schema '%s'", xsdFile));
                throw new XMLValidationException(String.format("Error(s) while parsing document:\n%s", request));
            }
        } catch (XMLValidationException e) {
            // Just throw away, we have already logged all there is to log
            throw e;
        } catch (Exception e) {
            // Print original exception
            // and log the xml schema used for validation
            LOGGER.error(String.format("Error(s) while parsing document:\n%s", request));
            LOGGER.error(String.format("The document is not valid with regards to the XML Schema '%s'", xsdFile));

            // Finally, pass on a new exception
            throw new XMLValidationException(String.format("Error(s) while parsing document:\n%s", request), e);
        }
    }

    /**
     * Cette methode permet de reinitialiser l'objet.
     */
    public void reset() {
        xmlErrorManager.reset();
    }

    /**
     * Cette methode permet de preciser le schema xsd a utiliser pour la validation.
     *
     * @param parser
     *            Le reader xml utilise a valider.
     * @param xsdFile
     *            Le nom du schema xsd a utiliser.
     * @throws SAXException
     *             En cas d'erreur. Cette exception est rattrappee par la methode appelante.
     */
    private void setNamespaceSchemaLocation(XMLReader parser, String xsdFile) throws SAXException {
        String property = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
        try {
            // specify the URI for Schema validation
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Setting property '%s' to value '%s'", property, xsdFile));
            }
            parser.setProperty(property, xsdFile);
        } catch (SAXException e) {
            // Do not add the exception to the log. This is done later on by
            // the caller method.
            LOGGER.debug(String.format("Setting property '%s' to value '%s'", property, xsdFile));

            // Just throw the same exception
            throw e;
        }
    }

    /**
     * renvoie le path du fichier xsd a utiliser pour la validation
     */
    public String getXsdFile() {
        return xsdFile;
    }
}
