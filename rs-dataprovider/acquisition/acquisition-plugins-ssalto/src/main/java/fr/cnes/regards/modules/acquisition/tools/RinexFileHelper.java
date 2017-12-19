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
package fr.cnes.regards.modules.acquisition.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;

/**
 * Cette classe permet de lire les fichier au format Rinex.
 * 
 * @author Christophe Mertz
 *
 */
public class RinexFileHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RinexFileHelper.class);

    /**
     * Chaine vide
     */
    private static final String EMPTY_STRING = "";

    /**
     * Fichier a lire, est initialise dans le constructeur
     */
    private final File currentFile;

    /**
     * filePattern permettant d'indentifier la ligne representant la date d'un bloc de mesure dans un fichier RINEX
     */
    private static final String MEASURE_BLOC_DATE_LINE_PATTERN = "\\s+([0-9]{2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\.([0-9]{7})(.)*";

    /**
     * Constructeur par defaut
     *
     * @param fileName
     */
    public RinexFileHelper(String fileName) {
        currentFile = new File(fileName);
    }

    /**
     * Constructeur
     *
     * @param aFile
     *
     */
    public RinexFileHelper(File aFile) {
        currentFile = aFile;
    }

    /**
     * Renvoie la chaine de caractere recuperee a la ligne lineNumber dans le groupe de capture catchGroup du filePattern pattern.
     *
     * @param lineNumber
     *            Si 0 ou -1 = Recherche de la ligne a partir d'un filePattern
     *            Si -2 = Recherche de la derniere ligne;
     *            Si >0 = Recherche de la ligne x dans le fichier
     * @param pattern
     * @param catchGroup
     * @return la chaine trouvée
     * @throws PluginAcquisitionException
     *             if get line return an empty {@link String}
     */
    public String getValue(int lineNumber, Pattern pattern, int catchGroup) throws PluginAcquisitionException {

        String value = null;
        if (lineNumber > 0) {
            String line = getLine(lineNumber, currentFile);
            LOGGER.debug("line read : " + line);
            Matcher valueMatcher = pattern.matcher(line);
            valueMatcher.matches();
            value = valueMatcher.group(catchGroup);
        } else if ((lineNumber == 0) || (lineNumber == -1)) {
            // all must be done using filePattern.
            // the first line which matches the filePattern contains the value
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                String line = "";

                while ((line = reader.readLine()) != null) {
                    Matcher valueMatcher = pattern.matcher(line);
                    if (valueMatcher.matches()) {
                        value = valueMatcher.group(catchGroup);
                        break;
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new PluginAcquisitionException(e);
            }
        }
        // get the file's last line
        else if (lineNumber == -2) {
            String line = getLine(lineNumber, currentFile);
            LOGGER.debug("line read : " + line);
            Matcher valueMatcher = pattern.matcher(line);
            valueMatcher.matches();
            value = valueMatcher.group(catchGroup);
        } else if (lineNumber == -3) {
            // all must be done using filePattern.
            // the last line which matches the filePattern contains the value
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    Matcher valueMatcher = pattern.matcher(line);
                    if (valueMatcher.matches()) {
                        value = valueMatcher.group(catchGroup);
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new PluginAcquisitionException(e);
            }
        }

        LOGGER.info("value : " + value + " found at line " + lineNumber + " and column " + catchGroup);
        return value;
    }

    /**
     * Retourne ligne dont le numero est lineNumber.
     * Si la fin du fichier est atteinte avant d'arriver a lineNumber, alors la valeur "" est renvoyee
     * Si lineNumber=-2, alors on renvoie la derniere ligne
     * Les valeurs de lineNumber = 0 et -1 sont a exclure de cette methode car la recherche de la ligne se fait d'apres un filePattern
     *
     * @param lineNumber
     *            le numero de la ligne a renvoyer
     * @param aFile
     *            le fichier a lire
     * @return a {@link String} that may be empty
     * @throws PluginAcquisitionException
     */
    private String getLine(int lineNumber, File aFile) throws PluginAcquisitionException {
        String line = EMPTY_STRING;
        try (BufferedReader reader = new BufferedReader(new FileReader(aFile))) {
            // get the last line
            String lastLine = EMPTY_STRING;
            if (lineNumber > 0) {
                // Go to line
                int count = 1;
                while ((count <= lineNumber) && (lastLine != null)) {
                    // Read line to go to next line
                    lastLine = reader.readLine();
                    count++;
                }
                // Set line value if end of stream not reached
                if (lastLine != null) {
                    line = lastLine;
                }
            } else if (lineNumber == -2) {
                // DM060 : get the last line
                while (lastLine != null) {
                    // Backup line (if empty file, line value = "")
                    line = lastLine;
                    // Read line
                    lastLine = reader.readLine();
                }
            } else {
                // Nothing to do
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new PluginAcquisitionException(e);
        }

        // If empty : throws a plugin exception
        if (line == null || line.isEmpty()) {
            String message = "No value found at this line (" + lineNumber + ")";
            throw new PluginAcquisitionException(message);
        }

        return line;
    }

    /**
     * Renvoie l'interval de date correspondant au min et max des dates des blocs de mesure.
     *
     * @return
     */
    public Interval getBlocMeasureDateInterval() {
        Pattern mesureBlocPattern = Pattern.compile(MEASURE_BLOC_DATE_LINE_PATTERN);
        Interval interval = new Interval();
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
            String line = reader.readLine();

            while (line != null) {
                Matcher matcher = mesureBlocPattern.matcher(line);
                if (matcher.matches()) {
                    long measureDate = getDate(matcher);
                    interval.update(measureDate);
                }
                line = reader.readLine();
            }

        } catch (IOException e) {
            LOGGER.error("unable to get date interval from file " + currentFile.getPath(), e);
        }
        return interval;
    }

    /**
     * Renvoie une date parsee par le Pattern de date de bloc.
     *
     * @param matcher
     * @return date en milliseconds
     */
    private long getDate(Matcher matcher) {

        OffsetDateTime odt = OffsetDateTime.of(Integer.parseInt("20" + matcher.group(1)),
                                               Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                                               Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                                               Integer.parseInt(matcher.group(6)), Integer.parseInt(matcher.group(7)),
                                               ZoneOffset.UTC);
        return 1000 * odt.toEpochSecond();
    }
}
