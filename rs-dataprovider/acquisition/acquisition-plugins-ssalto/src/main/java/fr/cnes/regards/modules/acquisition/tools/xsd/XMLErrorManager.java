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
package fr.cnes.regards.modules.acquisition.tools.xsd;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Cette classe permet de traiter les erreurs de validation.
 * 
 * @author Christophe Mertz
 */
public class XMLErrorManager implements ErrorHandler {

    /**
     * L'instance unique permettant de tracer la classe
     */
    private static Logger LOGGER = Logger.getLogger(XMLErrorManager.class);

    /**
     * Liste contenant les exceptions indiquant des warnings lors du parse. La liste contient des objets de la classe
     * <code>SAXParseException</code>.
     */
    private List<SAXParseException> warnings_ = null;

    /**
     * Liste contenant les exceptions indiquant des erreurs lors du parse. La liste contient des objets de la classe
     * <code>SAXParseException</code>.
     */
    private List<SAXParseException> errors_ = null;

    /**
     * Liste contenant les exceptions indiquant des erreurs graves lors du parse. La liste contient des objets de la
     * classe <code>SAXParseException</code>.
     */
    private List<SAXParseException> fatalErrors_ = null;

    //    /**
    //     * Parametre indiquant la mise en place ou non d'un log d'erreur specifique aux requetes de validation
    //     * 
    //     * @since 4.3
    //     */
    //    private boolean isValidationLoggerEnabled = false;

    public XMLErrorManager(boolean isValidationLoggerEnabled) {
        warnings_ = new ArrayList<>();
        errors_ = new ArrayList<>();
        fatalErrors_ = new ArrayList<>();
        //        isValidationLoggerEnabled = pIsValidationLoggerEnabled;
    }

    /**
     * Cette methode permet de reinitialiser l'objet.
     */
    public void reset() {
        warnings_ = new ArrayList<>();
        errors_ = new ArrayList<>();
        fatalErrors_ = new ArrayList<>();
    }

    /**
     * Permet de traiter les warning SAX.
     * 
     * @param exception
     *            L'exception a traiter.
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) {
        String message = String.format("Warning at line %d, column %d while parsing\n%s",
                                       Integer.toString(exception.getLineNumber()),
                                       Integer.toString(exception.getColumnNumber()), exception.getMessage());
        LOGGER.warn(message);

        //        // Log validation message in validation logger
        //        if (isValidationLoggerEnabled) {
        //            ErrorReport.warn(message);
        //        }

        // Do not throw any exception, but remember the warning
        warnings_.add(exception);
    }

    /**
     * Permet de traiter les erreurs SAX.
     * 
     * @param exception
     *            exception
     */
    @Override
    public void error(SAXParseException exception) {
        String message = String.format("Error at line %d, column %d while parsing\n%s", exception.getLineNumber(),
                                       exception.getColumnNumber(), exception.getMessage());
        LOGGER.error(message);

        //        // Log validation message in validation logger
        //        if (isValidationLoggerEnabled) {
        //            ErrorReport.error(message);
        //        }

        // Do not throw any exception. Continue to parse
        // the rest of the document. However, remember the error
        this.errors_.add(exception);
    }

    /**
     * Permet de traiter les erreurs fatales SAX.
     * 
     * @param exception
     *            exception
     */
    @Override
    public void fatalError(SAXParseException exception) {
        String message = String.format("Fatal error at line %d, column %d while parsing\n%s",
                                       Integer.toString(exception.getLineNumber()),
                                       Integer.toString(exception.getColumnNumber()), exception.getMessage());
        LOGGER.error(message);

        //        // Log validation message in validation logger
        //        if (isValidationLoggerEnabled) {
        //            ErrorReport.error(message);
        //        }

        // Do not throw any exception. Continue to parse
        // the rest of the document. However, remember the error
        this.fatalErrors_.add(exception);
    }

    public List<SAXParseException> getErrors() {
        return errors_;
    }

    public List<SAXParseException> getFatalErrors() {
        return fatalErrors_;
    }

    public List<SAXParseException> getWarnings() {
        return warnings_;
    }

}
