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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;

/**
 * Cette classe permet de lire les fichier au format Rinex.
 * 
 * @author Christophe Mertz
 *
 */
public class RinexFileHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RinexFileHelper.class);

    /**
     * Chaine vide
     */
    private static final String EMPTY_STRING = "";

    /**
     * fichier a lire, est initialise dans le constructeur
     */
    private File file_;

    /**
     * filePattern permettant d'indentifier la ligne representant la date d'un bloc de mesure dans un fichier RINEX
     */
    private static final String MEASURE_BLOC_DATE_LINE_PATTERN = "\\s+([0-9]{2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\s+([0-9]{1,2})\\.(.)*";

    /**
     * constructeur par defaut
     *
     * @param pFileName
     *            , le fichier a lire
     *
     */
    public RinexFileHelper(String pFileName) {
        file_ = new File(pFileName);
    }

    /**
     * constructeur
     *
     * @param pFile
     *
     */
    public RinexFileHelper(File pFile) {
        file_ = pFile;
    }

    /**
     * renvoie la chaine de caractere recuperee a la ligne pLineNumber dans le groupe de capture pCatchGroup du filePattern
     * pPattern.
     *
     * @param pLineNumber
     *            ( Si 0 ou -1 = Recherche de la ligne a partir d'un filePattern; Si -2 = Recherche de la derniere ligne; Si
     *            >0 = Recherche de la ligne x dans le fichier )
     * @param pPattern
     * @param pCatchGroup
     * @return
     * @throws PluginAcquisitionException
     *             if get line return an empty string
     */
    public String getValue(int pLineNumber, Pattern pPattern, int pCatchGroup) throws PluginAcquisitionException {

        String value = null;
        try {
            if (pLineNumber > 0) {
                String line = getLine(pLineNumber, file_);
                LOGGER.debug("line read : " + line);
                Matcher valueMatcher = pPattern.matcher(line);
                valueMatcher.matches();
                value = valueMatcher.group(pCatchGroup);
            } else if ((pLineNumber == 0) || (pLineNumber == -1)) {
                // all must be done using filePattern.
                // the first line which matches the filePattern contains the value
                BufferedReader reader = new BufferedReader(new FileReader(file_));
                String line = "";
                try {
                    while ((line = reader.readLine()) != null) {
                        Matcher valueMatcher = pPattern.matcher(line);
                        if (valueMatcher.matches()) {
                            value = valueMatcher.group(pCatchGroup);
                            break;
                        }
                    }
                } finally {
                    reader.close();
                }
            }
            // get the file's last line
            else if (pLineNumber == -2) {
                String line = getLine(pLineNumber, file_);
                LOGGER.debug("line read : " + line);
                Matcher valueMatcher = pPattern.matcher(line);
                valueMatcher.matches();
                value = valueMatcher.group(pCatchGroup);
            } else if (pLineNumber == -3) {
                // all must be done using filePattern.
                // the last line which matches the filePattern contains the value
                BufferedReader reader = new BufferedReader(new FileReader(file_));
                String line = "";
                try {
                    while ((line = reader.readLine()) != null) {
                        Matcher valueMatcher = pPattern.matcher(line);
                        if (valueMatcher.matches()) {
                            value = valueMatcher.group(pCatchGroup);
                        }
                    }
                } finally {
                    reader.close();
                }
            }

            LOGGER.info("value : " + value + " found at line " + pLineNumber + " and column " + pCatchGroup);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new PluginAcquisitionException(e);
        }
        return value;
    }

    /**
     * retourne ligne dont le numero est pLineNumber. Si la fin du fichier est atteinte avant d'arriver a pLineNumber,
     * alors la valeur "" est renvoyee Si pLineNumber=-2, alors on renvoie la derniere ligne. Les valeurs de pLineNumber
     * = 0 et -1 sont a exclure de cette methode car la recherche de la ligne se fait d'apres un filePattern
     *
     * @param pLineNumber
     *            le numero de la ligne a renvoyer
     * @param pFile
     *            le fichier a lire
     * @return a string that may be empty
     * @throws IOException
     */
    private String getLine(int pLineNumber, File pFile) throws PluginAcquisitionException {
        BufferedReader reader = null;
        String line = EMPTY_STRING;
        try {
            reader = new BufferedReader(new FileReader(pFile));
            // get the last line
            String lastLine = EMPTY_STRING;
            if (pLineNumber > 0) {
                // Go to line
                int count = 1;
                while ((count <= pLineNumber) && (lastLine != null)) {
                    // Read line to go to next line
                    lastLine = reader.readLine();
                    count++;
                }
                // Set line value if end of stream not reached
                if (lastLine != null) {
                    line = lastLine;
                }
            } else if (pLineNumber == -2) {
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
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Just log it
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        // If empty : throws a plugin exception
        if (EMPTY_STRING.equals(line)) {
            String message = "No value found at this line (" + pLineNumber + ")";
            throw new PluginAcquisitionException(message);
        }

        return line;
    }

    /**
     * renvoie l'interval de date correspondant au min et max des dates des blocs de mesure.
     *
     * @return
     */
    public Interval getBlocMeasureDateInterval() {
        Pattern mesureBlocPattern = Pattern.compile(MEASURE_BLOC_DATE_LINE_PATTERN);
        Interval interval = new Interval();
        try (BufferedReader reader = new BufferedReader(new FileReader(file_))) {
            String line = reader.readLine();

            while (line != null) {
                Matcher matcher = mesureBlocPattern.matcher(line);
                if (matcher.matches()) {
                    long measureDate = getDate(matcher);
                    interval.update(measureDate);
                }
                line = reader.readLine();
            }

        } catch (Exception e) {
            LOGGER.error("unable to get date interval from file " + file_.getPath());
        }
        return interval;
    }

    /**
     * renvoie un date parsee par le Pattern de date de bloc.
     *
     * @param pMatcher
     * @return
     */
    private long getDate(Matcher pMatcher) {
        Calendar myCalendar = Calendar.getInstance();
        myCalendar.set(Calendar.YEAR, Integer.parseInt("20" + pMatcher.group(1)));
        myCalendar.set(Calendar.MONTH, Integer.parseInt(pMatcher.group(2)) - 1);
        myCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(pMatcher.group(3)));
        myCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(pMatcher.group(4)));
        myCalendar.set(Calendar.MINUTE, Integer.parseInt(pMatcher.group(5)));
        myCalendar.set(Calendar.SECOND, Integer.parseInt(pMatcher.group(6)));
        myCalendar.set(Calendar.MILLISECOND, Integer.parseInt(pMatcher.group(7)));
        // the parser must be externally synchronized

        return myCalendar.getTimeInMillis();
    }

    public void releaseFile() {
        file_ = null;
    }

}
