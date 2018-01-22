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

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.tools.DateFormatter;

/**
 * Calculates a new date from a arc number parameter :<br> 
 * the calculate date is the 2002 18th June plus <code>(arc mumber - 1) * 7</code> days
 * 
 * @author Christophe Mertz
 *
 */
public class SetEnvisatD1BPoeCddisStartDate implements ICalculationClass {

    @Override
    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {
        Integer numeroArc = (Integer) value;

        Calendar cal = Calendar.getInstance();
        cal.set(2002, Calendar.JUNE, 18, 00, 00, 00);
        cal.add(Calendar.DAY_OF_YEAR, (numeroArc.intValue() - 1) * 7);

        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        return DateFormatter.getDateRepresentation(cal.getTime(), DateFormatter.XS_DATE_TIME_FORMAT);
    }

}
