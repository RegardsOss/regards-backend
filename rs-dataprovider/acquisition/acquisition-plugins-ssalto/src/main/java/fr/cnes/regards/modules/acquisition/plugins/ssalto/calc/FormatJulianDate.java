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

import java.util.Date;
import java.util.StringTokenizer;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.tools.CNESJulianDate;
import fr.cnes.regards.modules.acquisition.tools.DateFormatter;

/**
 * Prends une date en jour julien pour la mettre au format SIPAD.
 * 
 * @author Christophe Mertz
 *
 */
public class FormatJulianDate implements ICalculationClass {

    @Override
    public Object calculateValue(Object value, AttributeTypeEnum attributeType,
            PluginConfigurationProperties properties) {

        // Get Julian day and seconds
        StringTokenizer tokenizer = new StringTokenizer(value.toString(), " ");
        String julianDayString = null;
        String secondInDay = null;
        if (tokenizer.hasMoreTokens()) {
            julianDayString = tokenizer.nextToken();
        }
        if (tokenizer.hasMoreTokens()) {
            secondInDay = tokenizer.nextToken();
        }
        Date tmp = CNESJulianDate.toDate(julianDayString, secondInDay);
        String tmp2String = DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT);
        return tmp2String;
    }
}
