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

import java.util.Calendar;
import java.util.Date;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.PluginConfigurationProperties;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class SetDoris1BMOEFileCreationDate implements ICalculationClass {

    public SetDoris1BMOEFileCreationDate() {
        super();
    }

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {
        // The day after at midday
        Date oneDate = (Date) pValue;
        Calendar cal = Calendar.getInstance();

        cal.setTime(oneDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.HOUR_OF_DAY, 12);
        cal.add(Calendar.MINUTE, 00);
        cal.add(Calendar.SECOND, 00);

        return cal.getTime();
    }
}
