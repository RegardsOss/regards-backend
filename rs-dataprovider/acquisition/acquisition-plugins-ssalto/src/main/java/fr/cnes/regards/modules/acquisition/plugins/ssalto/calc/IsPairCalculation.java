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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;

/**
 * Cette classe renvoie Ascending si la valeur est paire et Descending sinon.
 * 
 * @author Christophe Mertz
 *
 */
public class IsPairCalculation implements ICalculationClass {

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {
        int value = Integer.parseInt(pValue.toString());
        int reste = value % 2;
        if (reste == 0) {
            return "Descending";
        } else {
            return "Ascending";
        }
    }

}
