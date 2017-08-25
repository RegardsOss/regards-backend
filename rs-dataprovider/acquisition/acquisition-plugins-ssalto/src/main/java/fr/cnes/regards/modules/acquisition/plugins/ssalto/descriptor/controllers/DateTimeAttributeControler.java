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

import java.util.Date;

import org.jdom.Element;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * Attribut de type DATE TIME
 * 
 * @author Christophe Mertz
 */
public class DateTimeAttributeControler extends AttributeControler {

    /**
     * Nom de l'element XML equivalent a l'objet.
     */
    public static final String XML_ELEMENT = "dateTimeAttribute";

    public DateTimeAttributeControler() {
        super();
    }

    @Override
    public String doGetXmlElement() {
        return XML_ELEMENT;
    }

    @Override
    public Element doGetValueAsString(Object pValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        value.addContent(String.valueOf(((Date) pValue).getTime()));
        return value;
    }

    /**
     * renvoie une representation de type String d'un objet pValue
     * 
     * @param pValue
     * @return
     */
    @Override
    public String doGetStringValue(Object pValue) {
        String outputValue;
        outputValue = String
                .valueOf(DateFormatter.getDateRepresentation((Date) pValue, DateFormatter.XS_DATE_TIME_FORMAT));
        return outputValue;
    }

    /**
     * Retourne la valeur de l'attribut.
     * 
     * @param pIndex
     * @return La valeur de l'attribut.
     * @throws DomainModelException
     *             l'index n'est pas valide
     */
    public Date getValue(DateTimeAttribute pAttribut, int pIndex) throws ModuleException {
        Date retour = (Date) getObjectValue(pAttribut, pIndex);
        return retour;
    }
}
