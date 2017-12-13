package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;

/**
 * Cette classe specialise la classe DateTimeAttribute du paquetage sipad.domain.model pour mettre a jour les valeurs de
 * la classe a partir des valeurs du fichier descripteur. La classe est interne du paquetage. Les classes qui utilise le
 * descriptor peut la traiter en tant que DateTimeAttribute.
 * 
 * @author Christophe Mertz
 */
public class DescriptorDateTimeAttribute extends DateTimeAttribute {

    /**
     * Cette constante donne le format des dates utilisees dans les descriptors.
     */
    private static final String DATE_ATTRIBUTE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorDateTimeAttribute.class);

    public DescriptorDateTimeAttribute() {
        super();
    }

    public DescriptorDateTimeAttribute(DateTimeAttribute pAttribute) {
        this();

        metaAttribute = pAttribute.getMetaAttribute();
        valueList = pAttribute.getValueList();
    }

    /**
     * Ajoute une valeur a l'attribut. La chaine en entree doit etre du format yyyy/MM/dd HH:mm:ss.SSS.
     * 
     * @param pValue
     *            La valeur a ajouter
     */
    public void addStringValue(String pValue) throws ModuleException {
        try {
            DateFormat parser = new SimpleDateFormat(DATE_ATTRIBUTE_FORMAT);
            parser.setLenient(false);
            addValue(parser.parse(pValue));
        } catch (ParseException e) {
            String msg = String.format("The '%s' does not match the '%s' format", pValue, DATE_ATTRIBUTE_FORMAT);
            LOGGER.error(msg, e);
            throw new ModuleException(msg, e);
        }
    }

}
