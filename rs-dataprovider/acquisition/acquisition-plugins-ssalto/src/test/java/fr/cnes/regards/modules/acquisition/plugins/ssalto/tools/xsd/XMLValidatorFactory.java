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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Cette classe permet de creer des objets de la classe <code>XMLValidation</code>.
 * 
 * Elle implemente l'interface PoolableObjectFactory. C'est a dire que la
 * classe permet de creer des objets <code>XMLValidation</code> a la demande d'un
 * <code>ObjectPool</code>.
 * 
 * @author Christophe Mertz
 */
public class XMLValidatorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLValidation.class);

    /**
     * Le chemin d'acces dans le classpath au schema utilise
     * pour valider les requetes. Ce chemin doit egalement etre declare dans
     * la classe <code>XsdResolver</code> pour que le parser puisse acceder
     * au fichier schema xsd.
     */
    private String xmlSchema = null;

    /**
     * Permet de definir la methode de resolution des URI des fichiers XML Schema
     * Notamment d'obliger le chargement de fichier dans un PATH particulier.
     */
    private EntityResolver resolver = null;

    /**
     * Parametre indiquant la mise en place ou non d'un log d'erreur specifique aux 
     * requetes de validation
     */
    protected boolean isValidationLoggerEnabled = false;

    public XMLValidatorFactory(EntityResolver pResolver) {
        super();
        resolver = pResolver;
    }

    /**
     * Cette methode cree un objet Digester contenant les regles des fichiers
     * retournes par la methode <code>doGetDigesterRuleFiles</code>.
     * 
     * @return Un objet Digester initialise avec des regles.
     * 
     * @throws java.lang.Exception En cas de probleme d'initialisation.
     */
    public Object makeObject() throws XMLValidationException {

        // use the already instantiated
        XMLValidation validator;
        try {
            validator = new XMLValidation(xmlSchema, resolver, isValidationLoggerEnabled);
            validator.reset();
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new XMLValidationException(e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New XML validator built");
        }

        return validator;
    }

    /**
     * Cette methode permet de reinitialiser l'objet utilise pour valider
     * une requete XML.
     * 
     * @param validator L'objet a reinitialiser
     * @return True si l'objet a pu etre reinitialise.
     */
    public boolean validateObject(Object validator) {

        // Reinitialize digester object
        ((XMLValidation) validator).reset();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("XML validator reinitialized");
        }

        return true;
    }

    public String getXmlSchema() {
        return xmlSchema;
    }

    public void setXmlSchema(String pName) {
        xmlSchema = pName;
    }

    public EntityResolver getResolver() {
        return resolver;
    }

    public void setResolver(EntityResolver newResolver) {
        resolver = newResolver;
    }

    /**
     * Positionne le flag de mise en place du logger de validation
     * @param enableLogValidation
     */
    public void setIsValidationLoggerEnabled(boolean enableLogValidation) {
        isValidationLoggerEnabled = enableLogValidation;
    }

}
