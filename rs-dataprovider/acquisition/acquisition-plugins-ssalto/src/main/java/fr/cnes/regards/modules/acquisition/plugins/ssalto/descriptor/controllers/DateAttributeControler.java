package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.util.Date;

import org.jdom.Element;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.DateAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

/**
 * Attribut de type DATE
 * 
 * @author Christophe Mertz
 */
public class DateAttributeControler extends AttributeControler {

    /**
     * Nom de l'element XML equivalent a l'objet.
     */
    public static final String XML_ELEMENT = "dateAttribute";

    public DateAttributeControler() {
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

    @Override
    public String doGetStringValue(Object pValue) {
        return String.valueOf(DateFormatter
                .getDateRepresentation((Date) pValue, DateFormatter.XS_DATE_FORMAT));
    }

    /**
     * Retourne la valeur de l'attribut.
     * 
     * @param pIndex
     * @return valeur de l'attribut
     * @throws DomainModelException
     * @since 5.1
     */
    public Date getValue(DateAttribute pAttribut, int pIndex) throws ModuleException {
        return (Date) getObjectValue(pAttribut, pIndex);
    }
}
