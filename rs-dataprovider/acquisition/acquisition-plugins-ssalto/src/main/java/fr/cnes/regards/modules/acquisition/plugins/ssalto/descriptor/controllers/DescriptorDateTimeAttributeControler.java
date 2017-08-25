/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 5.2 : DM : SIPNG-DM-0112-CN : 01/07/2012 : RIA
 * Creation
 *
 * FIN-HISTORIQUE
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jdom.Element;

import sipad.controlers.ControlersMessages;
import sipad.controlers.model.DateTimeAttributeControler;
import sipad.domain.file.DomainFileException;
import sipad.domain.model.DateTimeAttribute;

public class DescriptorDateTimeAttributeControler extends DateTimeAttributeControler {

    /**
     * Cette constante donne le format des dates utilisees dans les descriptors.
     * 
     * @since 3.0
     */
    private static final String DATE_ATTRIBUTE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * Attribut permettant la journalisation.
     * 
     * @since 3.0
     */
    static private Logger logger_ = Logger.getLogger(DescriptorDateTimeAttributeControler.class);

    /**
     * 
     * Constructeur
     * 
     * @since 5.2
     */
    public DescriptorDateTimeAttributeControler() {
        super();
    }

    /**
     * Ajoute une valeur a l'attribut. La chaine en entree doit etre du format yyyy/MM/dd HH:mm:ss.SSS.
     * 
     * @param pValue
     *            La valeur a ajouter
     * @since 3.0
     */
    public void addStringValue(DateTimeAttribute pAttribute, String pValue) throws DomainFileException {
        try {
            DateFormat parser = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
            parser.setLenient(false);
            pAttribute.addValue(parser.parse(pValue));
        }
        catch (ParseException e) {
            String msg = ControlersMessages.getInstance().getMessage("sipad.domain.descriptor.date.parse.error", pValue,
                                                                 DATE_ATTRIBUTE_FORMAT);
            logger_.error(msg, e);
            throw new DomainFileException(msg, e);
        }
    }

    /**
     * Cette methode doit retourner une presentation String de l'objet pValue. Dans le cas de cette methode pValue doit
     * etre de la classe <code>java.util.Date</code>.
     * 
     * @param pValue
     *            La valeur courante.
     * @return La valeur courante transformee en string.
     * @since 1.0
     */
    @Override
    public Element doGetValueAsString(Object pValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        DateFormat formater = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
        value.addContent(formater.format((Date) pValue));
        return value;
    }
}
