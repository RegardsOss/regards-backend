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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.tools.RinexFileHelper;

/**
 * Plugin specifique aux donnees jason2 GPSP10Flot Les attributs traites specifiquement sont les TIME_PERIOD, que l'on
 * va lire dans les blocs de mesure.
 *
 * @author Christophe Mertz
 */

public abstract class AbstractJasonGpsp10FlotProductMetadataPlugin extends AbstractJasonDoris10ProductMetadataPlugin {

    /**
     * A {@link DateTimeFormatter} for the pattern "yyyy-MM-dd HH:mm:ss"
     */
    protected static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AbstractJasonGpsp10FlotProductMetadataPlugin() {
        super();
    }

    @Override
    protected List<OffsetDateTime> getStartDateValue(Collection<File> files) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMinValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue > valueRead) {
                longValue = valueRead;
            }
        }
        List<OffsetDateTime> valueList = new ArrayList<>();
        Date newDate = new Date(longValue);
        valueList.add(OffsetDateTime.ofInstant(newDate.toInstant(), ZoneId.of("UTC")));
        return valueList;
    }

    @Override
    protected List<OffsetDateTime> getStopDateValue(Collection<File> files,
            Map<String, List<? extends Object>> attributeValueMap) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMaxValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue < valueRead) {
                longValue = valueRead;
            }
        }
        List<OffsetDateTime> valueList = new ArrayList<>();
        Date newDate = new Date(longValue);
        valueList.add(OffsetDateTime.ofInstant(newDate.toInstant(), ZoneId.of("UTC")));
        return valueList;
    }

    @Override
    protected List<OffsetDateTime> getCreationDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        OffsetDateTime creationDate = null;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            Pattern creationDatePattern = Pattern
                    .compile(".* ([\\d]{4}-[\\d]{2}-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}) .*");
            String dateStr = helper.getValue(2, creationDatePattern, 1);
            LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
            OffsetDateTime dateRead = OffsetDateTime.of(ldt, ZoneOffset.UTC);

            if (creationDate == null) {
                creationDate = dateRead;
            } else if (creationDate.isAfter(dateRead)) {
                creationDate = dateRead;
            }
        }
        valueList.add(creationDate);

        return valueList;
    }
}
