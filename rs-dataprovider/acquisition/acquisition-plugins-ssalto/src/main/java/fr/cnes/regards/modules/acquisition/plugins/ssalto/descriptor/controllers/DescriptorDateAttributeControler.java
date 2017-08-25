package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorDateAttribute;

public class DescriptorDateAttributeControler extends DateAttributeControler {

    /**
     * Cette constante donne le format des dates utilisees dans les descriptors.
     */
    private static final String DATE_ATTRIBUTE_FORMAT = "yyyy/MM/dd";

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorDateAttributeControler.class);

    public DescriptorDateAttributeControler() {
        super();
    }

    /**
     * Ajoute une valeur a l'attribut. La chaine en entree doit etre du format yyyy/MM/dd
     * 
     * @param pValue
     *            La valeur a ajouter
     */
    public void addStringValue(DescriptorDateAttribute pAttribute, String pValue) throws ModuleException {
        try {
            DateFormat parser = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
            parser.setLenient(false);
            pAttribute.addValue(parser.parse(pValue));
        } catch (ParseException e) {
            String msg = String.format("The '%s' does not match the '%s' format", pValue, DATE_ATTRIBUTE_FORMAT);
            LOGGER.error(msg, e);
            throw new ModuleException(msg, e);
        }
    }

    @Override
    public Element doGetValueAsString(Object pValue) {
        Element value = new Element(XML_ELEMENT_VALUE);
        DateFormat formater = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
        value.addContent(formater.format((Date) pValue));
        return value;
    }
}
