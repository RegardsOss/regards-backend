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
 * Met en forme la version passee en parametre XYYab sous le format DORIS=X.YY - DIODE=a.b
 * pour le cas particulier DA_TC_ENVISAT_LOGVOL_DORIS_2G 
 * @author Christophe Mertz
 */

public class SetEnvisatLogvolDoris2GSeparator implements ICalculationClass {

    public SetEnvisatLogvolDoris2GSeparator() {
        super();
    }

    @Override
    public Object calculateValue(Object newValue, AttributeTypeEnum type, PluginConfigurationProperties properties) {

        String value = (String) newValue;
        String version = new String("");

        version = "DORIS=" + value.substring(0, 1) + "." + value.substring(1, 3) + " - DIODE=" + value.substring(3, 4)
                + "." + value.substring(4, 5);

        return version;
    }
}
