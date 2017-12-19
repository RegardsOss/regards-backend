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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Cette classe permet de resoudre la localisation physique de fichiers XML Schema a partir de leur nom logique.
 * 
 * Ce gestionnaire permet a la validation XML d'aller chercher les bons fichiers sur le disque. Les URI des fichiers XML
 * Schema sont parametres dans le fichier de configuration des espaces d'ingestion traditionnellement places sous
 * /sipad/service/srv/ingestion/repository.
 * 
 * @author Christophe Mertz
 */
public class IngestXsdResolver implements EntityResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestXsdResolver.class);

    /**
     * Ce hashmap permet de faire la correspondance entre tous les fichiers xsd du projet sipad et leur chemin d'acces.
     * Les fichiers doivent etre accessible dans le classpath de l'application.
     * 
     * Cet objet est cree lors de la creation du premier objet XsdResolver.
     */
    protected static Map<String, File> xsdMap = new HashMap<>();

    /**
     * Constructeur par defaut
     */
    public IngestXsdResolver() {
        super();
    }

    /**
     * Cette methode permet de charger un fichier xsd se trouvant sur le disque accessible au service ingestion.
     * 
     * @param publicId
     *            L'identifiant public de l'entite externe referencee.
     * @param systemId
     *            L'identifiant systeme de l'entite externe referencee.
     * @return Un objet InputStream ouvert sur l'entite.
     * @throws org.xml.sax.SAXException
     *             En cas de probleme.
     * @throws java.io.IOException
     *             En cas de probleme.
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("public id : %s - system id : %s", publicId, systemId));
        }

        // Get file name of the xsd file to load
        // Only use name of file to chose which xsd to load.
        // It is not nice but at least it works
        File xsdFile = null;
        String xsdFileName = null;

        try {
            xsdFile = new File(new URI(systemId));
            xsdFileName = xsdFile.getName();
        } catch (URISyntaxException e) {
            String msg = String.format("Cannot get the filename from the uri '%s'", systemId);
            LOGGER.error(msg, e);
            throw new SAXException(msg, e);
        }

        // Get the access path of the xsd file if any
        File fileName = xsdMap.get(xsdFileName);
        if (fileName == null) {
            // Error: unknown xsd file.
            // Only known xsd file may be used for validation
            String msg = String.format("The xsd file '%s' cannot be loaded", systemId);
            LOGGER.error(msg);
            throw new SAXException(msg);
        }

        // Open stream to file
        // If the input is null the file does not exist
        InputStream stream = new FileInputStream(fileName);

        return new InputSource(stream);
    }

    /**
     * @param dictionary
     */
    public void addDictionary(File dictionary) {
        if (!xsdMap.containsKey(dictionary.getName())) {
            xsdMap.put(dictionary.getName(), dictionary);
        }
    }
}
