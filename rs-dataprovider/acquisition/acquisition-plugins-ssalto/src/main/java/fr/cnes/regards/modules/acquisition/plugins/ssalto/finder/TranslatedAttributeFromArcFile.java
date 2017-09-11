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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.CNESJulianDate;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.DateFormatter;

public class TranslatedAttributeFromArcFile extends FileNameFinder {

    protected static final int START_JULIAN_DAY_GROUP = 1;

    protected static final int START_HOUR_GROUP = 2;

    protected static final int STOP_JULIAN_DAY_GROUP = 3;

    protected static final int STOP_HOUR_GROUP = 4;

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    // Charset and decoder for ISO-8859-15
    private static Charset charset_ = Charset.forName("ISO-8859-15");

    private final CharsetDecoder decoder = charset_.newDecoder();

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {

        List<String> resultList = new ArrayList<>();

        // Get arc value from name
        @SuppressWarnings("unchecked")
        List<Integer> arcList = (List<Integer>) super.getValueList(fileMap, attributeValueMap);
        Integer arcValue = null;
        if (!arcList.isEmpty() && (arcList.size() == 1)) {
            arcValue = arcList.get(0);
        } else {
            throw new PluginAcquisitionException(
                    "Error while detecting arc value in filename for specified group number");
        }

        // Set searched filePattern
        String pattern = "[0-9]{6}[A-Z]{1}\\s([0-9]+)\\s([0-9]+)\\s([0-9]+)\\s([0-9]+)\\s" + "0*" + arcValue.toString()
                + "\\s.*";
        Pattern arcPattern = Pattern.compile(pattern);

        // Search for line in ARC reference file
        String arcFilePath = confProperties.getArcPath();
        String julianDay = null;
        String secondInDay = null;
        CharSequence charSeq;
        try {
            charSeq = readFile(arcFilePath);
            Matcher matcher = arcPattern.matcher(charSeq);
            if (matcher.find()) {
                if (getName().equals(START_DATE)) {
                    julianDay = matcher.group(START_JULIAN_DAY_GROUP);
                    secondInDay = matcher.group(START_HOUR_GROUP);
                } else if (getName().equals(STOP_DATE)) {
                    julianDay = matcher.group(STOP_JULIAN_DAY_GROUP);
                    secondInDay = matcher.group(STOP_HOUR_GROUP);
                }

            }
        } catch (IOException e) {
            throw new PluginAcquisitionException(e);
        }

        // Get date
        Date tmp = CNESJulianDate.toDate(julianDay, secondInDay);
        resultList.add(DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT));

        return resultList;
    }

    /**
     * Lit un fichier sous forme de charBuffer
     * 
     * @param filePath
     * @return
     * @throws IOException
     */
    private CharBuffer readFile(String filePath) throws IOException {
        FileInputStream fis = null;
        FileChannel fc = null;
        CharBuffer cb;
        try {
            // Open the file and then get a channel from the stream
            fis = new FileInputStream(filePath);
            fc = fis.getChannel();

            // Get the file's size and then map it into memory
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

            // Decode the file into a char buffer
            cb = decoder.decode(bb);

        } finally {
            if (fc != null) {
                fc.close();
            }
            if (fis != null) {
                fis.close();
            }
        }

        return cb;
    }
}
