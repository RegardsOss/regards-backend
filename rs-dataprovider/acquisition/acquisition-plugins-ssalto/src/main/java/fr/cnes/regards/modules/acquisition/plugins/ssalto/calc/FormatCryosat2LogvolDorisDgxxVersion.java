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

public class FormatCryosat2LogvolDorisDgxxVersion implements ICalculationClass {

    private static String DELIM = "_";

    private static int VERSION_DIGIT_LENGTH = 1;

    private static String POINT = ".";

    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {

        // Parse XYY_abb_ aaaammjj_hhmmss_MEM.REF
        // to DORIS=X.YY  DIODE=a.bb
        String str = (String) value;
        int index = str.indexOf(DELIM);
        String dorisInfo = str.substring(0, index);
        String diodeInfo = str.substring(index + 1);

        // Construct the result string
        StringBuffer buffer = new StringBuffer(24);
        buffer.append("DORIS=");
        buffer.append(dorisInfo.substring(0, VERSION_DIGIT_LENGTH));
        buffer.append(POINT);
        buffer.append(dorisInfo.substring(VERSION_DIGIT_LENGTH));
        buffer.append(" - DIODE=");
        buffer.append(diodeInfo.substring(0, VERSION_DIGIT_LENGTH));
        buffer.append(POINT);
        buffer.append(diodeInfo.substring(VERSION_DIGIT_LENGTH));

        return buffer;
    }
}
