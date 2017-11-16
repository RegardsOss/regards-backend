/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 5.5.2 : DM : SIPNG-DM-165-CN : 27/11/2015 : Ajout du format TIME_ONLY_FORMAT
 * VERSION : 5.2 : DM : SIPNG-DM-0112-CN : 01/07/2012 : RIA
 *
 * VERSION : 2012/04/18 : 5.1 : CS
 * FA-ID : SIPNG-DM-RIA-CN : 2012/04/18 : Merge Sadic.
 *
 * VERSION : 2011/09/05 : 5.0 : CS
 * DM-ID : SIPNG-DM-0099-CN : 2011/09/05 : Conversion en UTF-8
 *
 * VERSION : 2010/03/08 : 4.4.1 : CS
 * FA-ID : SIPNG-FA-0566-CN : 2010/03/08 : correction nommage fichiers statistiques
 *
 * VERSION : 2009/06/09 : 4.4 : CS
 * FA-ID : SIPNG-FA-0410-CN : 2009/06/09 : ajout du format DATE_HOUR_MIN_FORMAT pour le nom des fichiers descripteurs.
 * DM-ID : SIPNG-DM-0059-CN : 2009/11/16 : ajout format DATE_TIMESTAMP et DATE_FORMAT pour le client de statistique.
 *
 * VERSION : 2007/08/01 : 3.4 : CS
 * DM-ID : SADIC : 2007/08/01 : Ajout des formats correspondant aux types xsd xs:date et xs:dateTime
 *
 * VERSION : 2006/04/10 : 3.0 : CS
 * DM-ID : SIPNG-DM-0013-CN : 2006/04/10 : CS : Creation. Code deplace depuis sipad.client.web.common.tagbean.GuiDate.
 *
 * FIN-HISTORIQUE
 */
package fr.cnes.regards.modules.acquisition.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.DateUtilException;

/**
 * Cette classe contient des fonctionnalites permettant de formatter une date et d'en verifier le format. Elle expose un
 * ensemble de formats utilisables.
 * 
 * @author Christophe Mertz
 *
 */
public class DateFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateFormatter.class);

    /**
     * Format le plus complet possible (à la milliseconde pres) pour les dates/heures dans les IHMs
     */
    public static final  String FULL_DATE_TIME_FORMAT = "FULL_DATE_TIME_FORMAT";

    /**
     * Format de timestamp sans milliseconde
     */
    public static final  String DATE_TIMESTAMP = "DATE_TIMESTAMP";

    /**
     * Format le plus complet possible (à la milliseconde pres) pour les dates/heures dans les IHMs mais sans separateur
     */
    public static final  String FULL_DATE_TIMESTAMP = "FULL_DATE_TIMESTAMP";

    /**
     * Format commun pour les dates/heures dans les IHMs
     */
    public static final  String DATE_TIME_FORMAT = "DATE_TIME_FORMAT";

    public static final  String DATE_TIME_FORMAT_DDMMYYYY = "DATE_TIME_FORMAT_DDMMYYYY";

    public static final  String DATE_FORMAT_DDMMYYYY = "DATE_FORMAT_DDMMYYYY";

    /**
     * Format simple pour les dates sans heure dans les IHMs
     */
    public static final  String DATE_ONLY_FORMAT = "DATE_ONLY_FORMAT";

    /**
     * Format simple donnant uniquement la composant "heure" de la date fournie
     */
    public static final  String TIME_ONLY_FORMAT = "TIME_ONLY_FORMAT";

    /**
     * Format simple pour les dates sans heure dans les IHMs
     */
    public static final  String DATE_FORMAT = "DATE_FORMAT";

    /**
     * Format simple pour les dates sans heure pour l'ingestion.
     */
    public static final  String XS_DATE_FORMAT = "XS_DATE_FORMAT";

    /**
     * Format commun pour les dates/heures pour l'ingestion.
     */
    public static final  String XS_DATE_TIME_FORMAT = "XS_DATE_TIME_FORMAT";

    /**
     * format tout attache qui s'arrete aux minutes pour les noms de fichiers
     */
    public static final  String DATE_HOUR_MIN_FORMAT = "DATE_HOUR_MIN_FORMAT";

    /**
     * format avec separateur qui s'arrete aux minutes
     */
    public static final  String DATE_HOUR_MIN_FORMAT_SEPARATED = "DATE_HOUR_MIN_FORMAT_SEPARATED";

    /**
     * format avec separateur complet
     */
    public static final  String DATE_HOUR_FULL_FORMAT_SEPARATED = "DATE_HOUR_FULL_FORMAT_SEPARATED";

    /**
     * Map qui contient les informations sur les formats reconnus par la classe GuiDate (attribut statique et
     * initialisation statique)
     */
    private static Map<String, Format> formats = null;
    static {
        // formats declaration
        formats = new HashMap<>();
        // date and time down to the millisecond
        formats.put(FULL_DATE_TIME_FORMAT,
                     new Format("yyyy/MM/dd-HH:mm:ss.SSS", "^\\d{4}/\\d{2}/\\d{2}-\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$"));
        // date and time
        formats.put(DATE_TIMESTAMP, new Format("yyyyMMdd_HHmmss", "^\\d{4}\\d{2}\\d{2}_\\d{2}\\d{2}\\d{2}$"));
        // date and time down to the millisecond
        formats.put(FULL_DATE_TIMESTAMP,
                     new Format("yyyyMMddHHmmssSSS", "^\\d{4}\\d{2}\\d{2}\\d{2}\\d{2}\\d{2}\\d{3}$"));
        // date and time down to the second
        formats.put(DATE_TIME_FORMAT,
                     new Format("yyyy/MM/dd-HH:mm:ss", "^\\d{4}/\\d{2}/\\d{2}-\\d{2}:\\d{2}:\\d{2}$"));
        // date and time down to the second
        formats.put(DATE_TIME_FORMAT_DDMMYYYY,
                     new Format("dd/MM/yyyy HH:mm:ss", "^\\d{2}/\\d{2}/\\d{4}-\\d{2}:\\d{2}:\\d{2}$"));
        formats.put(DATE_FORMAT_DDMMYYYY, new Format("dd/MM/yyyy", "^\\d{2}/\\d{2}/\\d{4}$"));
        // date only (no time)
        formats.put(DATE_FORMAT, new Format("yyyyMMdd", "^\\d{4}\\d{2}\\d{2}$"));
        // date only (no time)
        formats.put(DATE_ONLY_FORMAT, new Format("yyyy/MM/dd", "^\\d{4}/\\d{2}/\\d{2}$"));
        // xs date and time down to the second
        formats.put(XS_DATE_TIME_FORMAT,
                     new Format("yyyy-MM-dd'T'HH:mm:ss", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"));
        // xs date only (no time)
        formats.put(XS_DATE_FORMAT, new Format("yyyy-MM-dd", "^\\d{4}-\\d{2}-\\d{2}$"));

        formats.put(DATE_HOUR_MIN_FORMAT, new Format("yyyyMMddHHmm", "^\\d{4}\\d{2}\\d{2}\\d{2}\\d{2}$"));
        formats.put(DATE_HOUR_MIN_FORMAT_SEPARATED, new Format("yyyyMMdd-HHmm", "^\\d{4}\\d{2}\\d{2}-\\d{2}\\d{2}$"));
        formats.put(DATE_HOUR_FULL_FORMAT_SEPARATED,
                     new Format("yyyyMMdd-HHmmss", "^\\d{4}\\d{2}\\d{2}-\\d{2}\\d{2}\\d{2}$"));
        formats.put(TIME_ONLY_FORMAT, new Format("HH:mm:ss", "^\\d{2}:\\d{2}:\\d{2}$"));
    }

    protected DateFormatter() {
        super();
    }

    /**
     * Cette methode permet de parser une date/heure. La methode est thread-safe. Il est preferable de verifier la
     * validite de la chaine de caracteres avec isValid() ou checkValid() avant d'appeler cette methode.
     *
     * @param date
     *            La chaine a parser.
     * @param formatId
     *            L'identifiant du format de date a utiliser. Il doit etre connu (isKnownFormat(pFormatId) == true).
     * @return La date correspondante.
     * @throws DateUtilException
     *             Si la chaine ne peut pas etre parsee
     */
    public static Date parse(String date, String formatId) throws DateUtilException {
        try {
            Date result = null;
            // get format information
            final Format format = getFormat(formatId);
            // get string parser
            final DateFormat parser = format.getParsingDateFormat();
            // the parser must be externally synchronized
            synchronized (parser) {
                result = parser.parse(date);
            }
            return result;
        } catch (final ParseException e) {
            throw new DateUtilException(e);
        }
    }

    /**
     * Renvoie la representation en chaine de caracteres de la date en fonction du format demande. La methode est
     * thread-safe.
     *
     * @param date
     *            La date a formatter.
     * @param formatId
     *            L'identifiant du format de date a utiliser. Il doit etre connu (isKnownFormat(pFormatId) == true).
     * @return Une chaine de caractere representant la date formattee. Chaine vide si date nulle en entree.
     */
    public static String getDateRepresentation(Date date, String formatId) {
        String result = "";
        if (date != null) {
            // get format information
            Format format = null;
            format = getFormat(formatId);
            // get string formatter
            final DateFormat parser = format.getParsingDateFormat();
            // the parser must be externally synchronized
            synchronized (parser) {
                result = parser.format(date);
            }
        }
        return result;
    }

    /**
     * Indique si une chaine de caractere satisfait a un certain format de date et si la date ainsi reconnue est valide.
     * La methode est thread-safe. Semblable a checkValid() sauf que checkValid() lance une exception en cas de non
     * validite
     *
     * @param date
     *            La chaine de caractere a tester.
     * @param formatId
     *            L'identifiant du format de date. Il doit etre connu (isKnownFormat(pFormatId) == true)
     * @return true si la chaine de caractere est au format demande; false sinon.
     */
    public static boolean isValid(String date, String formatId) {
        boolean isValid = false;
        try {
            checkValid(date, formatId);
            isValid = true;
        } catch (DateUtilException e) { // NOSONAR
            LOGGER.warn(e.getMessage());
            // DateUtilException has been thrown because the date is invalid
            isValid = false;
            // and swallow the exception
        }
        return isValid;
    }

    /**
     * Valide le fait qu'une chaine de caractere satisfait a un certain format de date et si la date ainsi reconnue est
     * valide. La methode est thread-safe. Semblable a isValid() sauf que isValid() retourne un booleen indiquant la
     * validite
     *
     * @param date
     *            La chaine de caractere a tester.
     * @param formatId
     *            L'identifiant du format de date. Il doit etre connu (isKnownFormat(pFormatId) == true)
     * @throws DateUtilException
     *             si la chaine n'est pas une date correspondant au format demandé
     */
    public static void checkValid(String date, String formatId) throws DateUtilException {
        // get format information
        final Format format = getFormat(formatId);

        // 1) syntax validation
        // get string validator
        final Pattern pattern = format.getValidatingPattern();
        // do the matching
        final Matcher matcher = pattern.matcher(date);
        if (!matcher.matches()) {
            
            final String msg = String.format("No match found"); 
            throw new DateUtilException(msg);
        }

        // 2) value validation
        // get string parser
        final DateFormat parser = format.getParsingDateFormat();
        try {
            // the parser must be externally synchronized
            synchronized (parser) {
                parser.parse(date);
            }
        } catch (final ParseException e) {
            // ParseException has been thrown because the date is invalid
            throw new DateUtilException(e);
        }
    }

    /**
     * Indique si un identifiant de format est reconnu
     *
     * @param formatId
     *            un identifiant de format
     * @return true si l'identifiant de format est connu; false sinon
     */
    public static boolean isKnownFormat(String formatId) {
        final Format result = formats.get(formatId);
        return (result != null);
    }

    /**
     * Renvoie l'objet Format correspondant a un identifiant de format.
     *
     * @param formatId
     *            un identifiant de format. <b>Il doit etre connu (isKnownFormat(pFormatId) == true)</b>
     * @return un objet Format non null
     * @throws IllegalArgumentException
     *             si le format est inconnu (isKnownFormat(pFormatId) == false). N'est pas sense arriver.
     */
    protected static Format getFormat(String formatId) {
        final Format result = formats.get(formatId);
        if (result == null) {
            final String msg = String.format("Unknow date formatter '%s'", formatId);
            LOGGER.error(msg);
            // reaching here is a programming error because one should use isKnownFormat(pFormatId)
            // to ensure that the format identifier is known
            throw new IllegalArgumentException(msg);
        }
        return result;
    }

    /**
     * Classe interne qui contient des informations relatives a un format de date Cette classe est interne a GuiDate car
     * aucun de ses aspects n'a besoin d'etre accessible a l'exterieur de GuiDate
     */
    protected static class Format {

        /**
         * Le format de parsing des dates
         */
        private final DateFormat parsingDateFormat;

        /**
         * Le format de validation des dates
         */
        private final Pattern validatingPattern;

        /**
         * Cree un objet etant donne un motif de parsing et un motif de validation
         *
         * @param parsingPattern
         * @param validatingPattern
         */
        public Format(String newParsingPattern, String newValidatingPattern) {
            // create date parsing format
            parsingDateFormat = new SimpleDateFormat(newParsingPattern);
            parsingDateFormat.setLenient(false);
            // create date validation regexp
            validatingPattern = Pattern.compile(newValidatingPattern);
        }

        /**
         * Retourne un objet servant a parser une date <b>ATTENTION: l'objet retourne n'est pas thread-safe; il faut
         * entourer son utilisation d'un bloc synchronized sur l'objet</b>
         *
         * @return un objet DateFormat
         */
        public DateFormat getParsingDateFormat() {
            return parsingDateFormat;
        }

        /**
         * Retourne un objet servant a valider une date
         *
         * @return un objet Pattern
         */
        public Pattern getValidatingPattern() {
            return validatingPattern;
        }
    }
}
