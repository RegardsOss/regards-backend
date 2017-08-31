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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class SetDorisRadicalFromName implements ICalculationClass {

    private String patternD = "^(.*)([a-z]{1})(D|S)([0-9]{8}_[0-9]{6})$";

    private String patternS = "^(.*)([a-z]{1})(D|S)([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$";

    private String DATE_SEPARATOR = "_";

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {

        String str = (String) pValue;
        String radical = "";

        Matcher matcherD = (Pattern.compile(patternD)).matcher(str);
        Matcher matcherS = (Pattern.compile(patternS)).matcher(str);
        if (matcherD.matches()) {
            radical = matcherD.group(1) + matcherD.group(3) + matcherD.group(4);
        } else if (matcherS.matches()) {
            radical = matcherS.group(1) + matcherS.group(3) + DATE_SEPARATOR + matcherS.group(5) + DATE_SEPARATOR
                    + matcherS.group(6);
        }

        return radical;
    }
}
