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

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.TranslatedFromCycleFileFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;

/**
 * Classe permettant de calculer le cycle associee a une date donnee au format specific lu dans les
 * fichiers cryosat2.
 * 
 * @author sbinda
 * @author Christophe Mertz
 *
 */
public class SetCryosat2Doris1bPoeCddisCycle implements ICalculationClass {

    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {
        String cycle = "";
        SetCryosat2Doris1bPoeCddisDate calculationDate = new SetCryosat2Doris1bPoeCddisDate();
        Date date = calculationDate.calculate(value, type);
        OffsetDateTime odt = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));

        if (date != null) {
            TranslatedFromCycleFileFinder finder = new TranslatedFromCycleFileFinder();
            finder.setAttributProperties(properties);

            try {
                String cycleFilePath = properties.getCycleFileFilepath();

                File cycleFile = new File(cycleFilePath);
                Integer intCycle = null;
                if (cycleFilePath.length() > 0 && cycleFile.exists()) {
                    // Compute value from cycle file first and orf file if necessary
                    intCycle = finder.getCycleOcurrence(odt);
                } else {
                    // Compute value from orf file only
                    intCycle = finder.getCycleOccurenceFromOrf(odt);
                }

                cycle = intCycle.toString();
            } catch (PluginAcquisitionException e) {
                cycle = "";
            }
        }

        return cycle;

    }

}
