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
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

/**
 * 
 * @author Christophe Mertz
 *
 */

public class SetEnvisatD1BPoeCddisCycleMin implements ICalculationClass {

    public SetEnvisatD1BPoeCddisCycleMin() {
        super();
    }

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {
        Integer numeroArc = (Integer) pValue;
        int temp;

        if ((numeroArc.intValue() % 5) != 0) {
            temp = numeroArc.intValue() / 5 + 7;
        }
        else {
            temp = numeroArc.intValue() / 5 + 6;
        }

        Integer minCycle = new Integer(temp);
        return minCycle;
    }
}
