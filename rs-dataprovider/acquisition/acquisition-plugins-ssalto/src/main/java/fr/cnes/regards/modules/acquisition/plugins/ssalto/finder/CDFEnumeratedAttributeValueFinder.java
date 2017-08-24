/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 2012/07/27 : 5.1 : CS
 * DM-ID : SIPNG-DM-0105-CN : 2012/07/27 : maj java7 et correction test
 *
 * VERSION : 2011/04/20 : 1.6 : CS
 * FA-ID : SIPNG-FA-0709-CN : 2011/04/20 : Ajout du parametre pluginProperties aux classes de calcul
 *
 * VERSION : 2011/04/04 : 1.4.4 : CS
 * FA-ID : SIPNG-FA-0709-CN : 2011/04/04 : Ajout du parametre pluginProperties aux classes de calcul
 *
 * VERSION : 2009/04/22 : 1.3 : CS
 * FA-ID : SIPNG-FA-0397-CN : 2009/04/22 : ajout de la liberation de la classe helper.
 *
 * VERSION : 2009/01/12 : 1.2 : CS
 * FA-ID : V12-FA-VR-FC-SSALTO-PDT-020-02 : 2009/03/23 : modification de l'interface
 * DM-ID : SIPNG-DM-0047-CN : 2009/01/12 : creation
 *
 * FIN-HISTORIQUE
 */

package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.NetCdfFileHelper;

/**
 * Ce finder a pour but de lister les valeurs possible prises par l'attribut de toutes les variables d'un fichier au
 * format NetCDF
 * 
 * @author CS
 * @version 1.2
 * @since 1.2
 */
public class CDFEnumeratedAttributeValueFinder extends CdfFileFinder {


    private static final Logger LOGGER = LoggerFactory.getLogger(CDFEnumeratedAttributeValueFinder.class);
    
    

    private List<String> exceptionList_;

    @Override
    public Attribute buildAttribute(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        LOGGER.debug("START building attribute " + getName());
        CompositeAttribute composedAttribute = new CompositeAttribute();
        // important, set to null, to not create a root element and add directly
        // the attributes
        // into the dataObjectDescription XML bloc.
        // composedAttribute.setName(null);
        try {
            List<Object> valueList = getValueList(pFileMap, pAttributeValueMap);
            // add attribut to calculated attribut map
            pAttributeValueMap.put(name, valueList);
            for (Object value : valueList) {
                if (calculationClass != null) {
                    value = calculationClass.calculateValue(value, getValueType(), confProperties);
                }
                // translate the value
                String translatedValue = changeFormat(value);
                List<String> translatedValueList = new ArrayList<>();
                translatedValueList.add(translatedValue);
                LOGGER.debug("adding value " + translatedValue);
                Attribute attribute = AttributeFactory.createAttribute(getValueType(), getName(), translatedValueList);
                composedAttribute.addAttribute(attribute);
            }
        }
        catch (DomainModelException e) {
            String msg = "unable to create attribute" + getName();
            throw new PluginAcquisitionException(msg, e);
        }
        LOGGER.debug("START building attribute " + getName());
        return composedAttribute;
    }

    /**
     * va chercher pour chaque fichier la valeur Methode surchargee
     */
    @Override
    public List<Object> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        List<Object> valueList = new ArrayList<>();
        for (File file : buildFileList(pFileMap)) {
            NetCdfFileHelper helper = new NetCdfFileHelper(file);
            for (String value : helper.getAllVariableAttributeValue(attributeName, exceptionList_)) {
                valueList.add(changeFormat(value));
            }
            helper.release();
        }
        return valueList;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append(" | exceptList : ");
        for (Iterator<String> gIter = exceptionList_.iterator(); gIter.hasNext();) {
            String except = gIter.next();
            buff.append(except);
            if (gIter.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    public void addException(String pExcept) {
        if (exceptionList_ == null) {
            exceptionList_ = new ArrayList<>();
        }
        exceptionList_.add(pExcept);
    }
}
