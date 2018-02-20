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
package fr.cnes.regards.modules.acquisition.finder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;

/**
 * Cette classe permet de passer d'une date a une occurence de cycle et vice versa. Elle utilise 2 fichiers pour cela
 * voir plusieurs dans un cas particulier. le fichier cycle et le(s) fichier(s) ORF
 * 
 * @author Christophe Mertz
 *
 */
public class TranslatedFromCycleFileFinder extends OtherAttributeValueFinder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedAttributeFromArcFile.class);

    /**
    * START_DATE attribute name
    */
    private static final String START_DATE = "START_DATE";

    /**
     * STOP_DATE attribute name
     */
    private static final String STOP_DATE = "STOP_DATE";

    /**
     *  {@link Charset} for ISO-8859-15
     */
    private static final Charset CHARSET = Charset.forName("ISO-8859-15");

    /**
     * {@link Charset} decoder for ISO-8859-15
     */
    private static final CharsetDecoder decoder = CHARSET.newDecoder();

    /**
     * Pattern de date dans un fichier de cycle
     */
    private static final String CYCLE_DATE_PATTERN = "\\d{2}-\\d{2}-\\d{4}";

    // change les \\s en \\{blank} et repasser les tests unitaires
    /**
     * Pattern pour les lignes dans le fichier cycle
     */
    private static final Pattern CYCLE_LINE_PATTERN = Pattern
            .compile("-\\s*(\\d{3})\\s*(" + CYCLE_DATE_PATTERN + ")\\s*(-|" + CYCLE_DATE_PATTERN + ")\\s*");

    /**
     * numero du groupe pour le groupe du numero de cycle
     */
    private static final int CYCLE_GROUP = 1;

    /**
     * DM60 - Prise en compte d'un cas particulier. Gestion du cycle lorsque la date est egale ou inferieure a la 1ere
     * date du cycle dans le fichier MISSION_CYCLES Dans ce cas il faut affiner la recherche avec les fichiers ORF
     * numero du groupe pour le groupe de la start date
     */
    private static final int CYCLE_START_GROUP = 2;

    /**
     * numero du groupe pour le groupe de la stop date
     */
    private static final int CYCLE_STOP_GROUP = 3;

    /**
     * Pattern de date dans un fichier orf
     */
    private static final String ORF_DATE_PATTERN = "\\d{4}/\\d{2}/\\d{2}";

    /**
     * Pattern de time dans un fichier de cycle
     */
    private static final String ORF_TIME_PATTERN = "\\d{2}:\\d{2}:\\d{2}.\\d{3}";

    /**
     * Pattern pour les lignes dans le fichier orf
     */
    private static final Pattern ORF_LINE_PATTERN = Pattern
            .compile("(" + ORF_DATE_PATTERN + "\\p{Blank}" + ORF_TIME_PATTERN + ")\\p{Blank}(\\d{3}).*");

    /**
     * numero du groupe pour le groupe du time dans le fichier orf
     */
    private static final int ORF_TIME_GROUP = 1;

    /**
     * numero du groupe pour le groupe du numero de cycle
     */
    private static final int ORF_CYCLE_GROUP = 2;

    /**
     * Format de la date dans le fichier orf
     */
    private static final DateTimeFormatter ORF_DATE_TIME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    /**
     * Log message
     */
    private static final String MSG_ORF_FILE_PATH = "getOrfFilePath : {}";

    /**
     * Format de la date dans les fichiers cycle
     */
    private static final DateTimeFormatter CYCLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Default constructor
     */
    public TranslatedFromCycleFileFinder() {
        super();
    }

    @Override
    public List<Object> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        final List<Object> otherValueList = super.getValueList(fileMap, attributeValueMap);
        // la liste doit etre de taille 1
        if (otherValueList.size() > 1) {
            final String msg = "Too much value for cycle attribut : " + super.getOtherAttributName();
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        // on applique le calcul sur les resultats
        final List<Object> processedValuesList = new ArrayList<>();
        final Object processValue = processCalculOnValues(otherValueList.get(0));
        processedValuesList.add(processValue);
        return processedValuesList;
    }

    /**
     * Calcul l'integer de l'occurence de cycle a partir de la date ou vice versa
     *
     * @param otherValue {@link Date} ou {@link Integer}
     * @return un {@link Integer} correspondant au numero de cycle ou une date correspondant au startDate
     * @throws PluginAcquisitionException attribut n'est pas du type {@link Date} ou {@link Integer}
     */
    private Object processCalculOnValues(Object otherValue) throws PluginAcquisitionException {
        // Integer or Date
        Object processedValue = null;
        // valueType est la valeur de sortie si c'est integer => on doit trouver
        // le cycle correspondant au start_date et vice versa
        if (valueType.equals(AttributeTypeEnum.TYPE_INTEGER)) {
            // Check if cycle file exists
            final String cycleFilePath = confProperties.getCycleFilePath();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cycle file path = {}", cycleFilePath);
            }

            boolean computeOrfFile = true;
            if ((cycleFilePath != null) && (cycleFilePath.length() > 0)) {
                final File cycleFile = new File(cycleFilePath);
                if (cycleFile.exists()) {
                    // Compute value from cycle file first and orf file if necessary
                    processedValue = getCycleOcurrence((OffsetDateTime) otherValue);
                    computeOrfFile = false;
                }
            }

            if (computeOrfFile) {
                // Compute value from orf file only
                processedValue = getCycleOccurenceFromOrf((OffsetDateTime) otherValue);
            }

        } else if (valueType.equals(AttributeTypeEnum.TYPE_DATE)
                || valueType.equals(AttributeTypeEnum.TYPE_DATE_TIME)) {
            final Integer cycleOccurence = (Integer) otherValue;
            if (getName().equals(START_DATE)) {
                processedValue = getCycleStartDate(cycleOccurence);
            } else if (getName().equals(STOP_DATE)) {
                processedValue = getCycleStopDate(cycleOccurence);
            }
        } else {
            final String msg = "Attribut Type must be INTEGER or DATE";
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }

        return processedValue;
    }

    /**
     * Retourne la date correspondant à la premiere ligne contenant l'occurence du cycle pCycleOccurence dans le fichier ORF.
     *
     * @param cycleOccurence l'occurence du cycle à chercher
     * @return la date correspondant à la premiere ligne contenant l'occurence du cycle pCycleOccurence dans le fichier ORF
     * @throws PluginAcquisitionException le cycle n'est pas trouvé dans le fichier ORF
     */
    private synchronized OffsetDateTime getCycleStartDate(Integer cycleOccurence) throws PluginAcquisitionException {
        OffsetDateTime cycleStartDate = null;
        int increment = 0;
        String cycleAsString = null;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cycle = {}", cycleOccurence.toString());
        }
        // occurence du cycle
        final NumberFormat numberFormat = new DecimalFormat("000");
        cycleAsString = numberFormat.format(cycleOccurence.intValue());

        // recupere le ou les fichiers depuis le disk
        final String[] getOrfFilePath = confProperties.getOrfFilePathPattern();
        while ((cycleStartDate == null) && (increment <= (getOrfFilePath.length - 1))) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MSG_ORF_FILE_PATH, getOrfFilePath[increment]);
            }

            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);

            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find() && (cycleStartDate == null)) {
                if (cycleAsString.equals(matcher.group(ORF_CYCLE_GROUP))) {
                    LocalDateTime ldt = LocalDateTime.parse(matcher.group(ORF_TIME_GROUP), ORF_DATE_TIME_FORMAT);
                    cycleStartDate = OffsetDateTime.of(ldt, ZoneOffset.UTC);
                }
            }
        }

        // le cycle n'a pas ete trouve
        if (cycleStartDate == null) {
            final String msg = "cycle not found in orf file : " + cycleAsString;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cycleStartDate found = {}", cycleStartDate);
        }
        return cycleStartDate;
    }

    /**
     * Retourne la date correspondant à la derniere ligne contenant l'occurence du cycle pCycleOccurence dans le fichier ORF
     *
     * @param cycleOccurence
     *            l'occurence du cycle
     * @return
     * @throws PluginAcquisitionException
     */
    private synchronized OffsetDateTime getCycleStopDate(Integer cycleOccurence) throws PluginAcquisitionException {
        OffsetDateTime cycleStopDate = null;
        int increment = 0;
        String cycleAsString = null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cycle = {}", cycleOccurence.toString());
        }

        // occurence du cycle +1
        final NumberFormat numberFormat = new DecimalFormat("000");
        cycleAsString = numberFormat.format(cycleOccurence.intValue());

        // recupere le ou les fichiers depuis le disk
        // DM SIPNG-DM-0060-CN : Prise en compte de x fichiers ORF
        final String[] getOrfFilePath = confProperties.getOrfFilePathPattern();
        while ((cycleStopDate == null) && (increment <= (getOrfFilePath.length - 1))) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MSG_ORF_FILE_PATH, getOrfFilePath[increment]);
            }

            // recupere le fichier depuis le disk
            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);

            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find()) {
                if (cycleAsString.equals(matcher.group(ORF_CYCLE_GROUP))) {
                    LocalDateTime ldt = LocalDateTime.parse(matcher.group(ORF_TIME_GROUP), ORF_DATE_TIME_FORMAT);
                    cycleStopDate = OffsetDateTime.of(ldt, ZoneOffset.UTC);
                }
            }
        }
        // le cycle n'a pas ete trouve
        if (cycleStopDate == null) {
            final String msg = "cycle not found in orf file : " + cycleAsString;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cycleStopDate found = {}", cycleStopDate);
        }

        return cycleStopDate;
    }

    /**
     * Le nom des fichiers ORF evolue en fonction des mises a jours. </br>
     * On ne selectionne que le dernier modifie
     *
     * @param orfFilePathPattern  de preference en chemin absolu
     * @return
     * @throws PluginAcquisitionException
     */
    private CharBuffer getOrfFile(String orfFilePathPattern) throws PluginAcquisitionException {
        final File dir = new File(new File(orfFilePathPattern).getParent());
        final String filePattern = new File(orfFilePathPattern).getName();
        final FileFilter fileFilter = new WildcardFileFilter(filePattern);
        final File[] files = dir.listFiles(fileFilter);

        // recupere le dernier fichier modifier
        final SortedSet<File> fileSet = new TreeSet<>(Comparator.comparing(File::lastModified));

        // doit contenir au moins un fichier
        if (files == null) {
            final String msg = "No file found in dir " + dir.getAbsolutePath() + " for filePattern " + filePattern;
            throw new PluginAcquisitionException(msg);
        } else {
            for (final File file : files) {
                fileSet.add(file);
            }
        }

        // doit contenir au moins un fichier
        CharBuffer fileBuffer;
        if (fileSet.isEmpty()) {
            final String msg = "No file found in dir " + dir.getAbsolutePath() + " for filePattern " + filePattern;
            throw new PluginAcquisitionException(msg);
        } else {
            try {
                final File lastModifiedFile = fileSet.last();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("lastModifiedFile read = {}", lastModifiedFile.getAbsolutePath());
                }

                fileBuffer = readFile(lastModifiedFile.getAbsolutePath());
            } catch (final IOException e) {
                throw new PluginAcquisitionException(e);
            }
        }

        return fileBuffer;
    }

    /**
     * Retourne l'occurence du cycle correspondant a la date
     *
     * @param startDate la date pour laquelle on cherche le cycle correspondant
     * @return l'occurence du cycle correspondant a la Date
     * @throws PluginAcquisitionException erreur de lecture du fichier contenant les cycles
     */
    public synchronized Integer getCycleOcurrence(OffsetDateTime startDate) throws PluginAcquisitionException {
        // recupere le fichier depuis le disk
        CharBuffer fileBuffer = null;
        try {
            fileBuffer = readFile(confProperties.getCycleFilePath());
        } catch (final IOException e) {
            throw new PluginAcquisitionException(new File(confProperties.getCycleFilePath()).getAbsolutePath(), e);
        }

        // convertit la date pour la comparaison
        final String stringpStartDate = CYCLE_DATE_FORMAT.format(startDate);
        // parcours le fichier ligne par ligne
        final Matcher matcher = CYCLE_LINE_PATTERN.matcher(fileBuffer);

        // look in ORF file
        boolean orfCycle = false;
        Integer cycleOccurence = null;
        int firstStartDate = 0;

        while (matcher.find() && (cycleOccurence == null)) {

            if (firstStartDate == 0) {
                // Si la date passee en parametre est identique ou
                // inferieure a la PREMIERE date de debut du premier cycle
                // alors affiner la recherche en cherchant parmi les
                // fichiers ORF ( cas des cycles 000 pour jason ou 998 et 999 pour jason2 )
                final String startDateString = matcher.group(CYCLE_START_GROUP);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("startDateString = {}", startDateString);
                }

                firstStartDate = 1;

                LocalDate cycleDate = LocalDate.parse(startDateString, CYCLE_DATE_FORMAT);

                if (LocalDate.from(startDate).isBefore(cycleDate) || LocalDate.from(startDate).isEqual(cycleDate)) {
                    cycleOccurence = getCycleOccurenceFromOrf(startDate);
                    orfCycle = true;
                }
            }

            if (!orfCycle) {

                // si la date cherchee est superieure a la stopDate on passe a la igne suivante
                final String stopDateString = matcher.group(CYCLE_STOP_GROUP);

                // cas du dernier cycle
                if ("-".equals(stopDateString)) {
                    cycleOccurence = Integer.valueOf(matcher.group(CYCLE_GROUP));
                } else if (stopDateString.equals(stringpStartDate)) {
                    // Les dates sont egales sans la precision des heures
                    // il faut regarder le 2eme fichier
                    cycleOccurence = getCycleOccurenceFromOrf(startDate);
                } else {
                    LocalDate cycleDate = LocalDate.parse(stopDateString, CYCLE_DATE_FORMAT);

                    if (LocalDate.from(startDate).isAfter(cycleDate)) {
                        // rien a faire
                    } else {
                        // La date est entre la stopDate de la ligne precedente =
                        // startDate de la ligne courante
                        // et la stopDate de la ligne courante
                        cycleOccurence = Integer.valueOf(matcher.group(CYCLE_GROUP));
                    }
                }
            }
        }

        return cycleOccurence;
    }

    /**
     * Retrouve dans le fichier Orf l'occurence du cycle correspondant a la date
     *
     * @param startDate la date pour laquelle on cherche le cycle correspondant
     * @return l'occurence du cycle correspondant a la date
     * @throws PluginAcquisitionException erreur de lecture du fichier Orf
     */
    public synchronized Integer getCycleOccurenceFromOrf(OffsetDateTime startDate) throws PluginAcquisitionException {
        Integer cycleOccurence = null;
        int increment = 0;
        // indique que le bon cycle occurence a ete trouve
        boolean cycleFound = false;

        // recupere le fichier depuis le disk
        // DM-0060 : Prise en compte de plusieurs fichiers ORF
        final String[] getOrfFilePath = confProperties.getOrfFilePathPattern();

        while ((!cycleFound) && (increment <= (getOrfFilePath.length - 1))) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MSG_ORF_FILE_PATH, getOrfFilePath[increment]);
            }

            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);
            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find() && !cycleFound) {

                LocalDateTime cycleDateTime = LocalDateTime.parse(matcher.group(ORF_TIME_GROUP), ORF_DATE_TIME_FORMAT);

                // Si la date trouvee est anterieure on stocke l'occurence sinon
                // c'est la fin

                if (OffsetDateTime.of(cycleDateTime, ZoneOffset.UTC).isBefore(startDate)) {
                    // sauvegarde l'occurence
                    cycleOccurence = Integer.valueOf(matcher.group(ORF_CYCLE_GROUP));
                } else {
                    // dans le cas ou la pStartDate est anterieure a la premiere
                    // occurrence d'ORF
                    // on prend la 1ere
                    if (cycleOccurence == null) {
                        cycleOccurence = Integer.valueOf(matcher.group(ORF_CYCLE_GROUP));
                    }
                    cycleFound = true;

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("cycleDateTime = {}", cycleDateTime.toString());
                    }
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cycleOccurence found = {}", cycleOccurence);
        }

        return cycleOccurence;
    }

    /**
     * Read a file and return a {@link CharBuffer}
     * 
     * @param filePath the path to the file to read
     * @return a {@link CharBuffer} to the the current file
     * @throws IOException an error occurs then reading the file
     */
    private CharBuffer readFile(String filePath) throws IOException {
        CharBuffer cb = null;

        // Open the file and then get a channel from the stream
        try (FileInputStream fis = new FileInputStream(filePath)) {

            final FileChannel fc = fis.getChannel();

            // Get the file's size and then map it into memory
            final int sz = (int) fc.size();
            final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

            // Decode the file into a char buffer
            cb = decoder.decode(bb);

            fc.close();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw e;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }

        return cb;
    }

    protected Object getTranslatedValue(Object value) throws PluginAcquisitionException {
        return processCalculOnValues(value);
    }
}
