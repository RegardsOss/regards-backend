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
package fr.cnes.regards.modules.acquisition.domain.metamodel;

import java.util.List;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DomainMetaModelException;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;

/**
 * Cette classe modelise la notion de MetaAttribut. Un MetaAttribut correspond a une definition de type d'attribut.
 * Cette classe est utile par exemple pour mettre en oeuvre les fonctions permettant de recuperer la liste des attributs
 * definis pour une entite.
 * 
 * @author Christophe Mertz
 *
 */
public class MetaAttribute {

    /**
     * Type de valeur defini par le meta-attribut
     */
    private AttributeTypeEnum valueType = null;

    /**
     * Le nom de l'attribut. Cet attribut est egalement utilise pour identifier l'objet de facon unique dans la base de
     * donnees
     */
    protected String name = null;

    /**
     * liste des differentes valeurs du meta attribut
     */
    protected List<Attribute> distinctValues = null;

    /**
     * Cet attribut designe si le meta attribute est lie a une methode de calcul permettant de trouver les valeurs de
     * ses instances dans une definition d'entite ou d'entite partagee. Ex : j'ai un jeu de donnees de type
     * DESCRIPTION_DATASET_WITH_ORBIT. La definition de l'attribut ORBIT dans DESCRIPTION_DATASET_WITH_ORBIT indique
     * qu'il y a un calcul "MIN". Alors la valeur de ORBIT dans le jeu de donnees sera le minimum des valeurs de ORBIT
     * dans les objets de donnees contenu par le jeu.
     */
    protected CalculationFunctionTypeEnum computationRule = null;

    /**
     * Default constructor
     */
    public MetaAttribute() {
        super();
    }

    /**
     * Constructor
     * 
     * @param pType
     *            le type du meta attribut
     */
    public MetaAttribute(AttributeTypeEnum pType) {
        valueType = pType;
    }

    /**
     * Constructor
     * 
     * @param pName
     *            Nom ou identifiant du meta-attribut.
     * @param pType
     *            Classe de la valeur du meta-attribut.
     */
    public MetaAttribute(String pName, AttributeTypeEnum pType) {
        name = pName;
        valueType = pType;
    }

    public String getName() {
        return name;
    }

    public AttributeTypeEnum getValueType() {
        return valueType;
    }

    public void setName(String pString) {
        name = pString;
    }

    public void setValueType(AttributeTypeEnum pEnum) {
        valueType = pEnum;
    }

    public void setValueType(String pEnum) throws DomainModelException {
        valueType = AttributeTypeEnum.parse(pEnum);
    }

    public CalculationFunctionTypeEnum getComputationRule() {
        return computationRule;
    }

    public void setComputationRule(String strCalculation) throws DomainMetaModelException {
        computationRule = CalculationFunctionTypeEnum.fromString(strCalculation);
    }

    public List<Attribute> getDistinctValues() {
        return distinctValues;
    }

    public void setDistinctValues(List<Attribute> pDistinctValues) {
        distinctValues = pDistinctValues;
    }

    @Override
    public boolean equals(Object pToBeCompared) {
        boolean ret = false;
        if (pToBeCompared instanceof MetaAttribute) {
            MetaAttribute att2 = (MetaAttribute) pToBeCompared;
            if (att2.getName().equals(getName()) && (att2.getValueType() == getValueType())) {
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public int hashCode() { // NOSONAR
        // getName() is consistent and ValueType is an Enum which Object.hashcode point to a static reference
        // the hashcode is thus consistent
        if ((getName() != null) && (getValueType() != null)) {
            return getName().hashCode() + getValueType().hashCode();
        } else if (getName() != null) {
            return getName().hashCode();
        } else {
            return 0;
        }
    }

}