package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import java.io.File;
import java.util.Date;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.TranslatedFromCycleFileFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;

/**
 * Classe permettant de calculer le cycle associee a une date donnee au format specific lu dans les
 * fichiers cryosat2.
 * @author sbinda
 *
 */
public class SetCryosat2Doris1bPoeCddisCycle implements ICalculationClass {

    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {
        String cycle = "";
        SetCryosat2Doris1bPoeCddisDate calculationDate = new SetCryosat2Doris1bPoeCddisDate();
        Date date = calculationDate.calculate(value, type);

        if (date != null) {
            TranslatedFromCycleFileFinder finder = new TranslatedFromCycleFileFinder();
            finder.setAttributProperties(properties);

            try {
                String cycleFilePath = properties.getCycleFileFilepath();

                File cycleFile = new File(cycleFilePath);
                Integer intCycle = null;
                if (cycleFilePath.length() > 0 && cycleFile.exists()) {
                    // Compute value from cycle file first and orf file if necessary
                    intCycle = finder.getCycleOcurrence(date);
                } else {
                    // Compute value from orf file only
                    intCycle = finder.getCycleOccurenceFromOrf(date);
                }

                cycle = intCycle.toString();
            } catch (PluginAcquisitionException e) {
                cycle = "";
            }
        }

        return cycle;

    }

}
