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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.RinexFileHelper;

/**
 * plugin specifiques au donnees jason2 GPSP10Flot Les attributs traites specifiquement sont les TIME_PERIOD, que l'on
 * va lire dans les blocs de mesure.
 *
 * @author Christophe Mertz
 */

public abstract class AbstractJasonGpsp10FlotProductMetadataPlugin extends AbstractJasonDoris10ProductMetadataPlugin {

    public AbstractJasonGpsp10FlotProductMetadataPlugin() {
        super();
    }

    /**
     * cree les attributs time_period et file_creation_date
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        super.doCreateIndependantSpecificAttributes(pFileMap, pAttributeMap);
    }

    @Override
    protected List<Date> getStartDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : pSsaltoFileList) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMinValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue > valueRead) {
                longValue = valueRead;
            }
        }
        List<Date> valueList = new ArrayList<>();
        valueList.add(new Date(longValue));
        return valueList;
    }

    @Override
    protected List<Date> getStopDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : pSsaltoFileList) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMaxValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue < valueRead) {
                longValue = valueRead;
            }
        }
        List<Date> valueList = new ArrayList<>();
        valueList.add(new Date(longValue));
        return valueList;
    }

    @Override
    protected List<Date> getCreationDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        Date creationDate = null;
        try {
            for (File file : pSsaltoFileList) {
                RinexFileHelper helper = new RinexFileHelper(file);
                Pattern creationDatePattern = Pattern
                        .compile(".* ([\\d]{4}-[\\d]{2}-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}) .*");
                String dateStr = helper.getValue(2, creationDatePattern, 1);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                Date dateRead = format.parse(dateStr);
                if (creationDate == null) {
                    creationDate = dateRead;
                } else if (creationDate.after(dateRead)) {
                    creationDate = dateRead;
                }
            }
            valueList.add(creationDate);
        } catch (ParseException e) {
            String msg = "unable to parse creation date";
            throw new PluginAcquisitionException(msg, e);
        }

        return valueList;
    }
}
