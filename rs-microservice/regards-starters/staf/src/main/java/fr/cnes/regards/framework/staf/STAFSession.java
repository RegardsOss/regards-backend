/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Cette classe represente une session de communication avec le STAF. Pour manipuler cette session, cette classe
 * encapsule le lancement d'un shell de pilotage permettant de passer les commandes STAF et de recuperer et d'analyser
 * les reponses correspondantes. Ce shell est lance sur appel de la methode stafconOpen et est arrete sur appel de la
 * methode stafconClose.
 * @author CS
 */
public class STAFSession {

    protected static Logger logger = Logger.getLogger(STAFSession.class);

    /**
     * Nom du shell utilise pour lancer la session STAF
     */
    private static final String SHELL = "/bin/ksh";

    /**
     * Commande d'initialisation du client STAF
     */
    private static final String INIT_STAF = "module load staf";

    /**
     * Commande d'initialisation du client d'un projet STAF GF (Gros Fichiers)
     */
    private static final String INIT_STAF_GF = "export VEM_NOEUD_STAF=ARCGF";

    /**
     * Commande d'ouverture d'une session STAF, avec affichage de la valeur de retour pour pouvoir tester la connexion
     */
    protected static final String STAFCON_OPEN = "stafcon -open -prj {0} -pw {1}; echo $?";

    /**
     * Identifie de façon unique la derniere partie de la commande staf d'ouverture ou est mentionne le mot de passe
     */
    protected static final String PASSWORD_SUFFIX = " -pw ";

    /**
     * Remplace pour cacher le mot de passe la chaine delimitee par la chaine <code>PASSWORD_SUFFIX</code>
     */
    protected static final String PASSWORD_REPLACEMENT = " -pw xxxxx";

    /**
     * Commande de fermeture d'une session STAF
     */
    private static final String STAFCON_CLOSE = "stafcon -close; echo $?";

    /**
     * Commande permettant de tester l'existence d'un fichier au STAF
     */
    private static final String STAFFIL_EXIST = "staffil -exist -f {0}";

    /**
     * Commande permettant de renommer un fichier au STAF
     */
    private static final String STAFFIL_MODIFY = "staffil -modify -stf {0},{1}";

    /**
     * Commande permettant de restituer un ensemble de fichiers du STAF
     */
    private static final String STAFFIL_RETRIEVE = "staffil -retrieve -stf {0}";

    /**
     * Commande permettant d archiver un ensemble de fichiers au STAF
     */
    private static final String STAFFIL_ARCHIVE = "staffil -archive -stf {0} -rep {1} -psc {2}";

    /**
     * Commande permettant de creer un noeud au STAF
     */
    private static final String STAFNODE_CREATE = "stafnod -create -n {0}";

    /**
     * deplacement dans l'arborescence d'un projet STAF
     */
    private static final String STAFNODE_LOCATE = "stafnod -locate -n {0}; echo $?";

    /**
     * suppression d'un fichier ou de plusieurs fichiers la liste de fichier peut se presenter de la maniere suivante :
     * /N1/FIC1 N2/FIC2 FIC3
     */
    private static final String STAFFIL_FILE_DELETE = "staffil -delete -f {0} -cnf n";

    /**
     * Option permettant de lancer la commande en asynchrone La vrai commande est " -asy Y". Pour le moement nous
     * utilisons "".
     */
    private static final String STAFFIL_ASYNCHRONE = "";

    /**
     * Commande permettant de lister les attributs d'un fichier au STAF
     */
    private static final String STAFFIL_LIST = "staffil -list -f {0} -att SIZE";

    private static final String STAF_STAT = "stafsta -list -stt 1";

    private static final String STAF_PRJ = "stafprj -list";

    /**
     * Cle permettant d'acceder a la taille d'un fichier dans la map de retour de la methode staffil_list.
     */
    public static final String STAF_ATTRIBUTE_SIZE = "SIZE";

    /**
     * Longueur maximale d'un noeud STAF.
     */
    public static final int NODE_MAX_LENGTH = 30;

    /**
     * Longueur maximale d'un fichier STAF.
     */
    public static final int FILE_MAX_LENGTH = 100;

    /**
     * Separateur des noms de fichiers
     */
    private static final String FILE_SEPARATOR = "/";

    /**
     * Format d'un nom de fichier STAF
     */
    private static final String FILE_PATTERN = "[^,;\\-/\\\\]+";

    /**
     * Longueur maximale d'un projet STAF.
     */
    public static final int PROJECT_MAX_LENGTH = 32;

    /**
     * Longueur maximale d'un mot de passe STAF.
     */
    public static final int PASSWORD_MAX_LENGTH = 16;

    /**
     * Longueur minimale d'un mot de passe STAF.
     */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /**
     * Nombre minimum de caracteres numeriques dans un mot de passe STAF.
     */
    public static final int PASSWORD_MIN_NUM = 2;

    /**
     * Format d'un mot de passe STAF (chaine de caracteres alphanumeriques comportant au minimum deux chiffres)
     */
    private static final String PASSWORD_PATTERN = "(\\w*\\d\\w*){2,}?";

    /**
     * Format d'un parametre alphanumerique STAF
     */
    private static final String ALPHANUM_PATTERN = "\\w+";

    /**
     * Format des messages d'erreur due a l'utilisateur retournes par le STAF
     */
    private static final String STAF_USER_ERROR_MESSAGE = "{0}STA{1} RC = {2} E {3}";

    /**
     * Format des messages d'erreur systeme retournes par le STAF
     */
    private static final String STAF_SYSTEM_ERROR_MESSAGE = "{0}STA{1} RC = {2} S {3}";

    /**
     * Format des messages de succes retournes par le STAF
     */
    private static final String STAF_SUCCESS_MESSAGE = "{0}STA{1} RC = 0 I {2}";

    /**
     * Format des messages indiquant que le fichier est deja present
     */
    private static final String STAF_ALREADY_ARCHIVED_MESSAGE = "{0}STA{1} RC = 10 E Fichier {2} deja archive";

    /**
     * Format des messages de succes retournes par le STAF pour une archive
     */
    private static final String STAF_SUCCESS_MESSAGE_ARCHIVE = "{0}STA{1} RC = 0 I Archivage de {2} effectue";

    private static final String STAF_FAIL_MESSAGE_DELETE = "{0}STA{1} RC = 9 E Fichier {2} inexistant ou droit insuffisant";

    /**
     * Format des tailles retournees par le STAF
     */
    private static final String STAF_SIZE_MESSAGE = "{0}SIZE(Ko)=''{1}''{2}";

    /**
     * Combien d'octets dans un millier d'octets
     */
    private static final int BYTES_IN_A_THOUSAND_BYTES = 1000;

    /**
     * Combien d'octets dans un kilo octet
     */
    private static final int BYTES_IN_A_KILO_BYTE = 1024;

    /**
     * Position du message d'erreur dans un STAF_USER_ERROR_MESSAGE ou un STAF_SYSTEM_ERROR_MESSAGE
     */
    private static final int ERROR_MESSAGE_POSITION = 3;

    private static final int ERROR_LEVEL_POSITION = 2;

    /**
     * Position du message de retour dans un STAF_SUCCESS_MESSAGE
     */
    private static final int SUCCESS_MESSAGE_POSITION = 2;

    /**
     * Position du message de retour dans un STAF_SUCCESS_MESSAGE_ARCHIVE
     */
    private static final int FILE_NAME_POSITION = 2;

    /**
     * Position du message de retour dans un STAF_SIZE_MESSAGE
     */
    private static final int SIZE_MESSAGE_POSITION = 1;

    /**
     * Format des messages Comunication en Cours avec le STAF
     */
    private static final String STAF_ONGOING_COMM_MESSAGE = "{0}TCPSO10 Communication en cours {1}";

    /**
     * Nombre de messages "Communication en Cours avec le STAF" maximum a attendre lors de la verification de la
     * connection
     */
    private static final int MAX_STAF_CURRENT_COMM_MSG = 10;

    /**
     * Shell utilise pour piloter la session STAF
     */
    private Process shellProcess;

    /**
     * Canal de commande
     */
    private PrintWriter commands;

    /**
     * Canal de reponse du STAF
     */
    private BufferedReader stafOutput;

    /**
     * STAF Configuration parameters
     */
    private final STAFConfiguration configuration;

    /**
     * Constructeur.
     */
    public STAFSession(STAFConfiguration pConfiguration) {
        shellProcess = null;
        commands = null;
        stafOutput = null;
        configuration = pConfiguration;
    }

    /**
     * Ouvre une session STAF en effectuant plusieurs tentatives en cas d'erreur
     *
     * @param pProject
     *            Projet STAF avec lequel on ouvre la session
     * @param pPassword
     *            Mot de passe utilise pour se connecter au STAF
     * @param pGFAccount
     *            Indique si le projet STAF est un GF (Gros Fichiers)
     * @throws STAFException
     *             Erreur de connexion au STAF
     */
    public void stafconOpen(String pProject, String pPassword) throws STAFException {

        int attemptsBeforeFailCount = 1;
        final int attemptsBeforeFail = configuration.getAttemptsBeforeFail().intValue();
        boolean attemptToConnect = true;

        while (attemptToConnect) {
            try {
                attemptToConnect = false;
                execStafconOpen(pProject, pPassword);
            } catch (final STAFException e) {
                stafconClose();
                // nouvelle tentative de connexion
                attemptToConnect = attemptsBeforeFailCount < attemptsBeforeFail;
                if (attemptToConnect) {
                    attemptsBeforeFailCount++;
                    final String msg = String.format("Attempts %d on %d to connect to the STAF",
                                                     attemptsBeforeFailCount, attemptsBeforeFail);
                    logger.info(msg);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Ouvre une session STAF
     *
     * @param pProject
     *            Projet STAF avec lequel on ouvre la session
     * @param pPassword
     *            Mot de passe utilise pour se connecter au STAF
     * @param pGFAccount
     *            Indique si le projet STAF est un GF (Gros Fichiers)
     * @throws STAFException
     *             Erreur de connexion au STAF
     */
    protected void execStafconOpen(String pProject, String pPassword) throws STAFException {
        // Prepare la commande STAF
        final MessageFormat stafconOpen = new MessageFormat(STAFCON_OPEN);
        final List<String> parameters = new ArrayList<>();
        parameters.add(pProject);
        parameters.add(pPassword);
        String command = stafconOpen.format(parameters.toArray());

        // Verification des parametres avant le lancement
        if (checkProject(pProject) && checkPassword(pPassword)) {
            // Lance le shell de pilotage de la session
            runSessionShell();
            // Initialise le client STAF et consomme le message correspondant
            // sur la sortie standard du shell.
            executeCommand(INIT_STAF);

            // password to not show it in logs
            String commandToLog = null;
            final int passwordSuffixPosition = command.lastIndexOf(PASSWORD_SUFFIX);

            if (passwordSuffixPosition > 0) {
                // we found the password position, we delete it and replace by
                // an other string
                commandToLog = command.substring(0, passwordSuffixPosition) + PASSWORD_REPLACEMENT;
            }

            // Execute la commande STAF de connexion
            executeCommand(command, commandToLog);
            checkConnection();

        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }
    }

    /**
     * Ferme une session STAF
     *
     * @throws STAFException
     *             si le flux de sortie du staf est illisible.
     */
    public void stafconClose() throws STAFException {
        // Execute la commande STAF de fermeture
        executeCommand(STAFCON_CLOSE);
        try {
            readSTAFLine();
            emptySTAFOutput();
        } catch (final STAFException e) {
            throw e;
        } finally {
            // Arrete le shell de pilotage
            if (shellProcess != null) {
                shellProcess.destroy();
                shellProcess = null;
                commands = null;
                stafOutput = null;
            }
        }
    }

    /**
     * Liste les attributs d'un fichier heberge au STAF. Les attributs listes sont les suivants :<br>
     * - Taille du fichier exprimee en kilo-octets. On notera que la taille du fichier indiquee par le STAF est exprimee
     * en milliers d'octets et une conversion avec arrondi par defaut est effectuee pour obtenir la taille en
     * kilo-octets. On notera egalement que la double troncature sur la taille du fichier, celle du STAF puis celle de
     * la conversion en kilo-octets entraine une perte de precision qui peut se traduire par une valeur inferieure d'un
     * kilo-octet a la taille reelle.
     *
     * @param pFilename
     *            Chemin d'acces complet au fichier dont on souhaite obtenir la valeur des attributs.
     * @return Une map contenant les valeurs retournees par le STAF.
     * @throws STAFException
     *             si la reponse contient un message d'erreur.
     */
    public Map<String, Integer> staffilList(String pFilename) throws STAFException {
        // Resultat de la fonction
        final HashMap<String, Integer> result = new HashMap<>();
        // Prepare la commande STAF
        final MessageFormat staffilList = new MessageFormat(STAFFIL_LIST);
        final List<String> parameters = new ArrayList<>();
        parameters.add(pFilename);
        String command = staffilList.format(parameters.toArray());

        // Verification des parametres avant le lancement
        if (checkLongFilename(pFilename)) {

            // Reponse du STAF
            String response;
            String errorMessage;
            Integer size;
            boolean responseAnalysed = false;

            // Execute la commande STAF
            executeCommand(command);

            // Analyse la reponse du STAF
            while (!responseAnalysed) {

                // Decode la ligne en cours
                response = readSTAFLine();
                errorMessage = checkErrorMessage(response);
                size = checkSizeInMessage(response);

                // Traite le resultat de l'analyse de la ligne en cours
                if (errorMessage != null) {
                    // Echec de la commande (le fichier n'existe peut etre pas)
                    final String msg = String.format("Unable to read the attributes of the file %s", pFilename);
                    logger.error(msg);
                    throw new STAFException(msg);
                } else if (size != null) {
                    // La taille du fichier a ete trouvee
                    responseAnalysed = true;
                    final int convertedSize = (size.intValue() * BYTES_IN_A_THOUSAND_BYTES) / BYTES_IN_A_KILO_BYTE;
                    result.put(STAF_ATTRIBUTE_SIZE, convertedSize);
                }

            }

            // Purge le flux de sortie du STAF
            emptySTAFOutput();
        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }

        return result;
    }

    public List<String> stafstat() throws STAFException {
        final List<String> result = new ArrayList<>();
        // Prepare la commande STAF
        String command = STAF_STAT;
        String response = "";

        boolean error = false;

        // Execute la commande STAF
        executeCommand(command);
        while ((response != null) && !("*****").equals(response)) {

            // Decode la ligne en cours
            response = readSTAFLine();
            final String errorMessage = checkErrorMessage(response);
            if (errorMessage != null) {
                logger.error(errorMessage);
                error = true;
                response = null;
            }
            if (response != null) {
                result.add(response);
            }
        }

        if (!error) {
            // Purge le flux de sortie du STAF
            emptySTAFOutput();

            command = STAF_PRJ;
            response = "";

            // Execute la commande STAF
            executeCommand(command);
            while ((response != null) && !("*****").equals(response)) {

                // Decode la ligne en cours
                response = readSTAFLine();
                if (response != null) {
                    result.add(response);
                }
            }
            // Purge le flux de sortie du STAF
            emptySTAFOutput();
        }

        return result;
    }

    /**
     * Verifie l'existence d'un fichier heberge au STAF.
     *
     * @param pFilename
     *            Chemin d'acces complet au fichier dont on souhaite tester l'existence.
     * @return Indicateur d'existence du fichier.
     * @throws STAFException
     *             si le nom du fichier est invalide.
     */
    public boolean staffilExist(String pFilename) throws STAFException {
        // Resultat de la fonction
        boolean exists = false;
        // Prepare la commande STAF
        final MessageFormat staffilExist = new MessageFormat(STAFFIL_EXIST);
        final List<String> parameters = new ArrayList<>();
        parameters.add(pFilename);
        String command = staffilExist.format(parameters.toArray());

        // Verification des parametres avant le lancement
        if (checkLongFilename(pFilename)) {

            // Reponse du STAF
            String response;
            String errorMessage;
            String successMessage;
            boolean responseAnalysed = false;

            // Execute la commande STAF
            executeCommand(command);

            // Analyse la reponse du STAF
            while (!responseAnalysed) {

                // Decode la ligne en cours
                response = readSTAFLine();
                errorMessage = checkErrorMessage(response);
                successMessage = checkSuccessMessage(response);

                // Traite le resultat de l'analyse de la ligne en cours
                if (errorMessage != null) {
                    // Le fichier n'existe pas
                    responseAnalysed = true;
                    exists = false;
                } else if (successMessage != null) {
                    // Le fichier existe
                    responseAnalysed = true;
                    exists = true;
                }

            }

            // Purge le flux de sortie du STAF
            emptySTAFOutput();
        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }

        return exists;
    }

    /**
     * Restitue un ensemble de fichiers archives au STAF.
     *
     * @param pFiles
     *            Ensemble des fichiers a restituer. Cet ensemble se presente sous la forme d'une map dont les cles sont
     *            les noms des fichiers au STAF (chemin compris) et les valeurs correspondantes les noms des fichiers
     *            issus du STAF (chemin compris).
     * @throws STAFException
     *             si la commande a echouee ou si le parametre pFiles est invalide.
     */
    public void staffilRetrieve(Map<String, String> pFiles) throws STAFException {

        // Prepare la commande STAF
        boolean validity = true;
        final MessageFormat staffilModify = new MessageFormat(STAFFIL_RETRIEVE);
        final Iterator<String> files = pFiles.keySet().iterator();
        final List<String> parameters = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        while (files.hasNext()) {
            final String stafFile = files.next();
            final String mdtFile = pFiles.get(stafFile);
            builder.append(stafFile + "," + mdtFile + " ");
            if (!checkLongFilename(stafFile)) {
                validity = false;
            }
        }
        parameters.add(builder.toString());
        String command = staffilModify.format(parameters.toArray());

        // Verification des parametres avant le lancement
        if (validity) {

            // Reponse du STAF
            String response;
            String errorMessage;
            String successMessage;
            int nbSuccess = 0;
            int nbFail = 0;

            // Execute la commande STAF
            executeCommand(command);

            // Analyse la reponse du STAF
            while ((nbSuccess + nbFail) < pFiles.size()) {

                // Decode la ligne en cours
                response = readSTAFLine();
                errorMessage = checkErrorMessage(response);
                successMessage = checkSuccessMessage(response);

                // Traite le resultat de l'analyse de la ligne en cours
                if (errorMessage != null) {
                    // Un fichier mal restitue de plus
                    nbFail++;
                    logger.error(errorMessage);
                } else if (successMessage != null) {
                    // Un fichier correctement restitue de plus
                    nbSuccess++;
                    logger.info(successMessage);
                }
            }

            // Purge le flux de sortie du STAF
            emptySTAFOutput();

            if (nbFail > 0) {
                String msg = "Unable to retrieve all files";
                logger.error(msg);
                throw new STAFException(msg);
            }

        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }
    }

    /**
     * Archive un ensemble de fichiers archives au STAF.<br>
     * Dans le cas de l'archivage au STAF, a cet etape la Map de fichier <code>pFiles</code> est limitee en taille et
     * est inferieure ou egale au nombre max de fichier transferable dans un flow.<br>
     * La methode STAFService#dispatchFilesInSeveralFlow(java.util.Map, int, String) est utilise systematiquement afin
     * de limiter la taille du flow.<br>
     *
     * @param pFiles
     *            Ensemble des fichiers a archiver. Cet ensemble se presente sous la forme d'une map dont les cles sont
     *            les noms des fichiers (chemin compris) et les valeurs correspondantes les noms des fichiers a archiver
     *            au STAF (chemin compris).
     * @param pServiceClass
     *            : la classe de service a utiliser pour les fichiers de ce flot.
     * @return une HashMap contenant les repertoire reels d'archivage pour chaque fichier passe en entree.
     * @throws STAFException
     *             si la commande a echouee ou si le parametre pFiles est invalide.
     */
    public List<String> staffilArchive(Map<String, String> pFiles, String pServiceClass, boolean pReplace)
            throws STAFException {
        final List<String> archivedFilesList = new ArrayList<>();

        // Prepare commande STAF command
        String command = null;
        boolean validity = true;
        final MessageFormat staffilArchive = new MessageFormat(STAFFIL_ARCHIVE);
        final Iterator<String> files = pFiles.keySet().iterator();
        final List<String> parameters = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        final Map<String, String> renamedLocalFilePathMap = new HashMap<>();
        // check if staf directory exists and create it otherwise
        checkAndCreateDirectory(pFiles.values());
        while (files.hasNext()) {
            String mdtFile = files.next();
            final String stafFile = pFiles.get(mdtFile);
            builder.append(mdtFile + "," + stafFile + " ");
            if (!checkLongFilename(stafFile)) {
                final String msg = String.format("Error file name is too long : %s", stafFile);
                logger.warn(msg);
                validity = false;
            }

        }
        parameters.add(builder.toString());
        if (pReplace) {
            parameters.add("y");
        } else {
            parameters.add("n");
        }
        parameters.add(pServiceClass);
        command = staffilArchive.format(parameters.toArray());
        // Asynchrone command
        command = command + STAFFIL_ASYNCHRONE;

        // Check parameters before launching
        if (validity) {

            // STAF answer
            String response = null;
            String errorMessage = null;
            String successMessage = null;
            String alreadyArchivedMessage = null;
            int nbSuccess = 0;
            int nbFail = 0;

            try {
                // Execute STAF command
                executeCommand(command);

                // Analyse STAF answer
                while ((nbSuccess + nbFail) < pFiles.size()) {

                    // Decode current line
                    response = readSTAFLine();

                    // check if the message says that the file is already archived
                    alreadyArchivedMessage = checkAlreadyArchivedMessage(response);
                    successMessage = checkSuccessMessage(response);
                    errorMessage = checkErrorMessage(response);

                    if (successMessage != null) {
                        // A file correctly returned more
                        nbSuccess++;
                        logger.info(successMessage);
                        // File location on STAF
                        String sourceFilePath = getFileNameFromResponse(response, STAF_SUCCESS_MESSAGE_ARCHIVE);
                        logger.info("File properly archived : " + sourceFilePath);

                        // Add file location in list
                        archivedFilesList.add(sourceFilePath);
                    } else if (alreadyArchivedMessage != null) {
                        // A file is already archived
                        nbFail++;
                        logger.warn(alreadyArchivedMessage);
                    } else if (errorMessage != null) {
                        final int errorLevel = getErrorMessageLevel(response);
                        if (errorLevel >= 6) {
                            // Failure of command
                            final String msg = "Unable to archive all files";
                            logger.error(msg);
                            throw new STAFException(msg);
                        }
                        // just a warning message
                        nbFail++;
                        logger.warn(errorMessage);

                    }
                }

                // Purge the output stream of STAF
                emptySTAFOutput();

                if (nbFail > 0) {
                    // Failure of command
                    final String msg = "Unable to archive all files";
                    logger.error(msg);
                }
            } catch (final Exception e) {
                throw e;
            } finally {
                renameStafFilesAfterArchiving(renamedLocalFilePathMap);
            }

        } else {
            // Invalid parameter
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }
        return archivedFilesList;
    }

    /**
     *
     * Retourne le chemin du fichier local archivé au STAF avant son renomage si cela a été réalisé, sinon retourne le
     * pSourceFilePath fournit en paramètre.
     *
     * @param pSourceFilePath
     * @param pRenamedLocalFilePathMap
     *            Map<OriginalFilePath, RenamedFilePath>
     * @return {@link String} Original file path
     */
    private String getSTAFOriginalFilePath(String pSourceFilePath, Map<String, String> pRenamedLocalFilePathMap) {
        // Default, the file was not rename so return the file path given
        String originalFilePath = pSourceFilePath;

        // If map containing file renamed befoire archiving is not null and not empty, get the original file path before
        // renaming
        if ((pRenamedLocalFilePathMap != null) && !pRenamedLocalFilePathMap.isEmpty()) {
            final Set<String> files = pRenamedLocalFilePathMap.keySet();
            for (final String curentOriginalFilePath : files) {
                // NOTE : This is not possible to have two files with the same renamed file path. An exception is thrown
                // before when the Map is created during the file rename action in method renameStafFileForArchiving
                final String curentRenamedFilePath = pRenamedLocalFilePathMap.get(curentOriginalFilePath);
                // If the renamed file path equals the file path given then return the originalFilePath associated
                if ((curentRenamedFilePath != null) && curentRenamedFilePath.equals(pSourceFilePath)) {
                    originalFilePath = curentOriginalFilePath;
                    break;
                }
            }
        }
        return originalFilePath;
    }

    /**
     *
     * Renomme les fichiers après archivage pour redonner leur nom d'origine aux fichiers si ceux-ci comportaient des
     * noms invalides pour le STAF.
     *
     * @param pRenamedLocalFilesPathMap
     *            Map<OriginalFilePath, RenamedFilePath>
     * @throws STAFException
     */
    private void renameStafFilesAfterArchiving(Map<String, String> pRenamedLocalFilesPathMap) throws STAFException {

        if ((pRenamedLocalFilesPathMap != null) && !pRenamedLocalFilesPathMap.isEmpty()) {
            final Set<String> files = pRenamedLocalFilesPathMap.keySet();
            for (final String originalFilePath : files) {
                // NOTE : This is not possible to have two files with the same renamed file path. An exception is thrown
                // before when the Map is created during the file rename action in method renameStafFileForArchiving
                final File file = new File(originalFilePath);
                final File fileToRename = new File(pRenamedLocalFilesPathMap.get(originalFilePath));
                logger.warn("Renaming file after archiving from : " + fileToRename.getPath() + " to " + file.getPath());
                if (!fileToRename.renameTo(file)) {
                    throw new STAFException(String.format("[STAF] Error renaming file %s to %s", fileToRename.getPath(),
                                                          file.getPath()));
                }
            }
        }
    }

    /**
     *
     * Renomage d'un nom de fichier avant archivage pour enlever les caracteres interdits au STAF.
     *
     * @param pMdtFile
     * @param pRenamedLocalFilesPathMap
     *            Map<OriginalFilePath, RenamedFilePath>
     * @return
     * @throws STAFException
     */
    private String renameStafFileForArchiving(String pMdtFile, Map<String, String> pRenamedLocalFilesPathMap,
            Boolean pRenamePhysicalFile) throws STAFException {
        final File file = new File(pMdtFile);
        final String originalfilePath = pMdtFile;
        String renamedFilePath = originalfilePath;

        // Check that the file to rename exists
        if (!pRenamePhysicalFile || file.exists()) {
            final String fileName = file.getName();

            // Check if there is forbiden caracters in the fileName of the file to rename
            if (fileName.contains("-")) {
                // Calculate new fileName
                final String newName = file.getName().replace('-', '_');
                final File newFile = new File(file.getParent(), newName);

                // If the physical rename is asked, move the file
                if (pRenamePhysicalFile.equals(Boolean.TRUE)) {
                    if (!pRenamedLocalFilesPathMap.containsValue(newFile.getPath())) {
                        if (!newFile.exists()) {
                            logger.warn("STAF file to archive contains invalid caracters. Renaming file "
                                    + file.getPath() + " to " + newFile.getPath() + " before archving");
                            if (!file.renameTo(newFile)) {
                                throw new STAFException(String.format("[STAF] Error renaming file %s to %s",
                                                                      file.getPath(), newFile.getPath()));
                            }
                            pRenamedLocalFilesPathMap.put(originalfilePath, newFile.getPath());
                        } else {
                            throw new STAFException(
                                    "STAF file rename error. Destination file already exists : " + newFile.getPath());
                        }
                    } else {
                        throw new STAFException(
                                "STAF file rename error. A previous file was renamed with the same result name for file "
                                        + fileName);
                    }
                }
                // return the new file path
                renamedFilePath = newFile.getPath();
            }
        } else {
            throw new STAFException("File to archive " + file.getPath() + " does not exists");
        }

        return renamedFilePath;
    }

    /**
     * verifie si le chemin existe et le cree si besoin On test si le chemin existe en essayant de se positionner dessus
     * Si il n'existe pas, on verifie chaque partie du chemin et on le cree si besoin enfin, on repositionne le
     * repertoire courant a la racine.
     *
     * @param pStafFile
     */
    private void checkAndCreateDirectory(String pStafPath) throws STAFException {

        final boolean directoryExists = checkDirectory(pStafPath);
        if (!directoryExists) {
            // create the missing directories
            final StringTokenizer pathTokenizer = new StringTokenizer(pStafPath, "/", false);
            while (pathTokenizer.hasMoreTokens()) {
                final String token = pathTokenizer.nextToken();
                if (!checkDirectory(token)) {
                    // create the directory and go into it
                    createStafNode(token);
                    checkDirectory(token);
                }
            }
        }
        // go back to root directory
        checkDirectory("/");

    }

    /**
     * verifie si le chemin existe et le cree si besoin On test si le chemin existe en essayant de se positionner dessus
     * Si il n'existe pas, on verifie chaque partie du chemin et on le cree si besoin enfin, on repositionne le
     * repertoire courant a la racine.
     *
     * @param pStafFile
     */
    private void checkAndCreateDirectory(Collection<String> pStafFileList) throws STAFException {
        final List<String> list = new ArrayList<>();
        for (Object element : pStafFileList) {
            String stafPath;
            final File stafFile = new File((String) element);
            stafPath = stafFile.getParent();
            if (!list.contains(stafPath)) {
                checkAndCreateDirectory(stafPath);
                list.add(stafPath);
            }
        }

    }

    /**
     * verifie le repertoire passe en parametre en essayant d'aller dans ce repertoire Attention, effet de bord : le
     * repertoire courant dans le staf est modifie si le repertoire existe.
     *
     * @param stafDirectory
     *            le repertoire a valider
     * @return true si le repertoire est accessible, false sinon
     * @throws STAFException
     */
    private boolean checkDirectory(String stafDirectory) throws STAFException {
        final MessageFormat staffilArchive = new MessageFormat(STAFNODE_LOCATE);
        final List<String> parameters = new ArrayList<>();
        parameters.add(stafDirectory);
        String command = staffilArchive.format(parameters.toArray());
        // Execute STAF command
        executeCommand(command);
        // Decode current line
        final String response = readSTAFLine();
        final String errorMessage = checkErrorMessage(response);
        boolean exists;
        // Action on analysis result of current line
        if (errorMessage != null) {
            // Read the integer response value
            readSTAFLine();
            logger.error(errorMessage);
            exists = false;
        } else {
            exists = true;
        }

        return exists;

    }

    /**
     * Renomme un fichier heberge au STAF.
     *
     * @param pOldFilename
     *            Chemin d'acces complet au fichier qu'on souhaite renommer.
     * @param pNewFilename
     *            Nom a donner au fichier (hors chemin d'acces)
     * @throws STAFException
     *             si le parametre est invalide ou si le STAF renvoie une reponse contenant un message d'erreur.
     */
    public void staffilModify(String pOldFilename, String pNewFilename) throws STAFException {

        // Prepare la commande STAF
        final MessageFormat staffilModify = new MessageFormat(STAFFIL_MODIFY);
        final List<String> parameters = new ArrayList<>();
        parameters.add(pOldFilename);
        parameters.add(pNewFilename);
        String command = staffilModify.format(parameters.toArray());

        // Verification des parametres avant le lancement
        if (checkLongFilename(pOldFilename) && checkShortFilename(pNewFilename)) {

            // Reponse du STAF
            String response;
            String errorMessage;
            String successMessage;
            boolean responseAnalysed = false;

            // Execute la commande STAF
            executeCommand(command);

            // Analyse la reponse du STAF
            while (!responseAnalysed) {

                // Decode la ligne en cours
                response = readSTAFLine();
                errorMessage = checkErrorMessage(response);
                successMessage = checkSuccessMessage(response);

                // Traite le resultat de l'analyse de la ligne en cours
                if (errorMessage != null) {
                    // Echec de la commande
                    final String msg = String.format("Unable to rename the file %s with the following name %s",
                                                     pOldFilename, pNewFilename);
                    logger.error(msg);
                    throw new STAFException(msg);
                } else if (successMessage != null) {
                    // Le fichier existe
                    responseAnalysed = true;
                }
            }

            // Purge le flux de sortie du STAF
            emptySTAFOutput();
        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }

    }

    /**
     * Restitue un ensemble de fichiers archives au STAF de maniere bufferisee (c'est a dire en decoupant le lot de
     * fichiers en plusieurs flots et en executant plusieurs commandes de restitution successives).
     *
     * @param pFiles
     *            Ensemble des fichiers a restituer. Cet ensemble se presente sous la forme d'une map dont les cles sont
     *            les noms des fichiers au STAF (chemin compris) et les valeurs correspondantes les noms des fichiers
     *            issus du STAF (chemin compris).
     * @throws STAFException
     *             si le parametre est invalide ou si le STAF renvoie une reponse contenant un message d'erreur.
     */
    public void staffilRetrieveBuffered(Map<String, String> pFiles) throws STAFException {
        // Iterator on files to return
        final Iterator<String> files = pFiles.keySet().iterator();

        while (files.hasNext()) {
            // All files are not archived in only one command.
            // We create intermediate flow to assur restitution granularity
            // which is
            // necessary to detect problems.
            final Map<String, String> set = new HashMap<>();
            int fileIndex = 0;
            // Prepare a files flow
            while ((fileIndex < configuration.getMaxStreamFilesRestitutionMode().intValue()) && files.hasNext()) {
                final String currentFile = files.next();
                set.put(currentFile, pFiles.get(currentFile));
                fileIndex++;
            }
            // Return wanted files flow
            staffilRetrieve(set);

        }
    }

    /**
     * Creation d un nouveau noeud sur le STAF
     *
     * @param pNode
     *            : noeud a creer au STAF
     * @throws STAFException
     *             si le parametre est invalide ou si le STAF renvoie une reponse contenant un message d'erreur.
     */
    public void createStafNode(String pNode) throws STAFException {
        // Prepare la commande STAF
        final MessageFormat stafnodeCreate = new MessageFormat(STAFNODE_CREATE);
        final List<String> parameters = new ArrayList<>();
        parameters.add(pNode);
        String command = stafnodeCreate.format(parameters.toArray());

        // Reponse du STAF
        String response;
        String errorMessage;
        String successMessage;

        // Execute la commande STAF
        executeCommand(command);

        // Decode la ligne en cours
        response = readSTAFLine();
        errorMessage = checkErrorMessage(response);
        successMessage = checkSuccessMessage(response);

        // Traite le resultat de l'analyse de la ligne en cours
        if (errorMessage != null) {
            // Un fichier mal restitue de plus
            logger.error(errorMessage);
        } else if (successMessage != null) {
            logger.info(successMessage);
        }

        // Purge le flux de sortie du STAF
        emptySTAFOutput();
    }

    /**
     * permet de supprimer une liste de fichiers .
     *
     * @param pFileList
     *            une list de chaines de caracteres presentant le chemin complet du fichier dans le projet STAF :
     *            exemple : /NOEUD1/NOEUD2/FICHIER1
     * @return une List contenant les noms de fichiers qui n'ont pas ete supprimes.
     * @throws STAFException
     */
    public List<String> staffilDelete(Set<Path> pSTAFFilePaths) throws STAFException {
        final List<String> notDeletedFiles = new ArrayList<>();
        String command = null;
        final MessageFormat staffilDelete = new MessageFormat(STAFFIL_FILE_DELETE);
        final List<String> parameters = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        boolean validity = true;

        for (final Iterator<Path> iter = pSTAFFilePaths.iterator(); iter.hasNext() && validity;) {
            final Path stafFile = iter.next();
            builder.append(stafFile);
            if (!checkLongFilename(stafFile.toString())) {
                validity = false;
            }
            builder.append(" ");
        }
        if (validity) {
            parameters.add(builder.toString());
            command = staffilDelete.format(parameters.toArray());

            // Execute la commande STAF
            executeCommand(command);
            // Analyse STAF answer
            int nbSuccess = 0;
            int nbFail = 0;
            while ((nbSuccess + nbFail) < pSTAFFilePaths.size()) {

                // Decode current line
                final String response = readSTAFLine();

                // check if the message says that the file is not found in staf
                final String successMessage = checkSuccessMessage(response);
                final String errorMessage = checkErrorMessage(response);

                if (successMessage != null) {
                    // A file correctly returned more
                    nbSuccess++;
                    logger.info(successMessage);
                } else if (errorMessage != null) {
                    // A file is not found int STAF : add the fileName to the
                    // list to return.
                    nbFail++;
                    final String sourceFilePath = getFileNameFromResponse(response, STAF_FAIL_MESSAGE_DELETE);
                    notDeletedFiles.add(sourceFilePath);
                    logger.warn(errorMessage);
                }
            }

            // Purge le flux de sortie du STAF
            emptySTAFOutput();
        } else {
            // Parametre invalide
            final String msg = String.format("Invalid parameter in the STAF command : %s", command);
            logger.error(msg);
            throw new STAFException(msg);
        }
        return notDeletedFiles;
    }

    /**
     * Lance un shell de pilotage de la session
     */
    protected void runSessionShell() throws STAFException {
        try {
            // Execute un shell de pilotage et encapsule l'acces aux
            // descripteurs IO
            shellProcess = Runtime.getRuntime().exec(SHELL);
            commands = new PrintWriter(shellProcess.getOutputStream());
            stafOutput = new BufferedReader(new InputStreamReader(shellProcess.getInputStream()));
        } catch (final IOException e) {
            // Erreur de lancement du shell de pilotage
            final String msg = String.format("An error occured while creating a STAF session shell %s", SHELL);
            logger.error(msg, e);
            throw new STAFException(msg, e);
        }
    }

    /**
     * Execute une commande au sein du Shell
     *
     * @param pCommand
     *            Commande executee
     * @param pTextForCommandLog
     *            texte a afficher dans le log, ce parametre est utile pour ne pas afficher une commande staf telle
     *            quelle (masquage mot de passe par ex.). SI ce texte est null, on affiche la commande
     */
    protected void executeCommand(String pCommand, String pTextForCommandLog) {
        if (commands != null) {
            // Trace the command
            if (pTextForCommandLog == null) {
                logger.info("STAF <= " + pCommand);
            } else {
                logger.info("STAF <= " + pTextForCommandLog);
            }
            // Submit the command
            commands.println(pCommand);
            commands.flush();
        }
    }

    /**
     * Execute une commande au sein du Shell. La commande est loggee telle quelle en mode debug (attention au mot de
     * passe).
     *
     * @param pCommand
     *            Commande executee
     */
    protected void executeCommand(String pCommand) {
        executeCommand(pCommand, null);
    }

    /**
     * Attente de l'obtention d'une connexion (la connexion n'est pas immediate). Elle se traduit par un un 0 en retour
     * dans le cas d'une connexion reussie et par un message d'erreur sur la sortie standard en cas d'echec et un retour
     * >0
     *
     * @throws STAFException
     *             Erreur de connexion
     */
    protected void checkConnection() throws STAFException {

        // Reponse du STAF
        String response;
        String errorMessage;
        String ongoingMessage;
        int responseCount = 0;
        boolean successMessage = false;
        // while the response is an ongoingComm message, re-read the response
        // but not over 10 times
        while ((responseCount < MAX_STAF_CURRENT_COMM_MSG) && !successMessage) {
            // read the line
            response = readSTAFLine();
            errorMessage = checkErrorMessage(response);
            ongoingMessage = checkOngoingCommunicationMessage(response);
            if (errorMessage != null) {
                final String msg = String.format("Unable to connect to the STAF : %s", errorMessage);
                logger.error(msg);
                throw new STAFException(msg);
            } else if (ongoingMessage != null) {
                // it is a ongoning communication message, increment the
                // response count not to
                successMessage = false;
                responseCount++;
            } else if (Integer.parseInt(response) == 0) {
                // it is ok, returnValue has been displayed and is over 0
                successMessage = true;
            } else if (Integer.parseInt(response) > 0) {
                final String msg = String.format("Unable to connect to the STAF : %s", errorMessage);
                logger.error(msg);
                throw new STAFException(msg);
            }
            if (responseCount == MAX_STAF_CURRENT_COMM_MSG) {
                // we get only erroMessages, or ongoing communication messages
                // connection failed
                final String msg = String.format("Unable to connect to the STAF : %s", errorMessage);
                logger.error(msg);
                throw new STAFException(msg);
            }
        }
        // Vide le flux de reponse du STAF
        emptySTAFOutput();
    }

    /**
     * Lit une ligne de reponse du STAF.
     *
     * @return La ligne lue sur la sortie du STAF
     * @throws STAFException
     *             Erreur de lecture du flux de sortie du shell de pilotage
     */
    protected String readSTAFLine() throws STAFException {
        String line = "";
        try {
            if (stafOutput != null) {
                // Read one line

                line = stafOutput.readLine();
                // Trace the response
                logger.info("STAF => " + line);
            }
        } catch (final IOException e) {
            // Erreur de lecture des reponses du STAF
            final String msg = "STAF response can not be read";
            logger.error(msg, e);
            throw new STAFException(msg, e);
        }

        return line;
    }

    /**
     * Verifie si la reponse du STAF est une erreur.
     *
     * @param response
     *            Reponse du STAF
     * @return Le message d'erreur, null si le parametre n'est pas une reponse d'erreur
     * @throws STAFException
     *             en cas d'erreur systeme
     */
    protected String checkErrorMessage(String pResponse) throws STAFException {

        // Un message null indique un echec de decodage qui signifie que la
        // reponse
        // du STAF n'est pas une erreur.
        String message = null;
        final MessageFormat userErrorFormat = new MessageFormat(STAF_USER_ERROR_MESSAGE);
        final MessageFormat systemErrorFormat = new MessageFormat(STAF_SYSTEM_ERROR_MESSAGE);

        try {
            message = (String) (userErrorFormat.parse(pResponse)[ERROR_MESSAGE_POSITION]);
        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a
            // une
            // erreur. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }
        try {
            if (message == null) {
                // Si le decodage d'une erreur utilisateur n'a pas donne de
                // resultat on
                // essaie de decoder une erreur systeme
                message = (String) (systemErrorFormat.parse(pResponse)[ERROR_MESSAGE_POSITION]);
                if (message != null) {
                    throw new STAFException(message);
                }
            }
        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a
            // une
            // erreur. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }

        return message;
    }

    /**
     * Verifie si la reponse du STAF est un warning ( numero d'erreur <=5).
     *
     * @param response
     *            Reponse du STAF
     * @return Le message d'erreur, null si le parametre n'est pas une reponse d'erreur
     * @throws STAFException
     *             en cas d'erreur systeme
     */
    protected int getErrorMessageLevel(String pResponse) throws STAFException {

        // Un message null indique un echec de decodage qui signifie que la
        // reponse
        // du STAF n'est pas une erreur.
        int level = 0;
        final MessageFormat userErrorFormat = new MessageFormat(STAF_USER_ERROR_MESSAGE);

        try {
            final String resultString = (String) userErrorFormat.parse(pResponse)[ERROR_LEVEL_POSITION];
            level = Integer.parseInt(resultString);

        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a
            // une
            // erreur. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }
        return level;
    }

    /**
     * Verifie si la reponse du STAF est un message de succes.
     *
     * @param response
     *            Reponse du STAF
     * @return Le message de succes, null si le parametre n'est pas une reponse de succes
     */
    protected String checkSuccessMessage(String pResponse) {

        // Un message null indique un echec de decodage qui signifie que la
        // reponse
        // du STAF n'est pas un succes.
        String message = null;
        final MessageFormat successFormat = new MessageFormat(STAF_SUCCESS_MESSAGE);

        try {
            message = (String) (successFormat.parse(pResponse)[SUCCESS_MESSAGE_POSITION]);

        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a un
            // succes. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }

        return message;
    }

    protected String checkAlreadyArchivedMessage(String pResponse) {
        // Un message null indique un echec de decodage qui signifie que la
        // reponse
        // du STAF n'est pas un succes.
        String message = null;
        final MessageFormat successFormat = new MessageFormat(STAF_ALREADY_ARCHIVED_MESSAGE);

        try {
            final String fileName = (String) (successFormat.parse(pResponse)[SUCCESS_MESSAGE_POSITION]);
            if (fileName != null) {
                message = "Fichier " + fileName + " deja archive";
            }
        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a un
            // succes. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }

        return message;
    }

    /**
     * recupere le nom d'un fichier dans un pattern de message
     */
    protected String getFileNameFromResponse(String pResponse, String pPattern) {
        String message = null;
        final MessageFormat successFormat = new MessageFormat(pPattern);
        try {
            message = (String) (successFormat.parse(pResponse)[FILE_NAME_POSITION]);
        } catch (final ParseException e) {
            logger.warn("Error parsing STAF response message");
        }

        return message;
    }

    /**
     * Verifie si la reponse du STAF indique la taille d'un fichier.
     *
     * @param response
     *            Reponse du STAF
     * @return La taille si elle a ete lue, null si le parametre n'est pas une reponse specifiant une taille
     */
    protected Integer checkSizeInMessage(String pResponse) {

        // Un message null indique un echec de decodage qui signifie que la
        // reponse
        // du STAF n'indique pas une taille.
        Integer size = null;
        String sizeString = null;
        final MessageFormat sizeFormat = new MessageFormat(STAF_SIZE_MESSAGE);

        try {
            sizeString = (String) (sizeFormat.parse(pResponse)[SIZE_MESSAGE_POSITION]);
            if (sizeString != null) {
                // Retire les espaces precedant et succedant eventuellement a la
                // taille
                sizeString = sizeString.trim();
                // Puis convertit la chaine lue en valeur entiere
                size = new Integer(sizeString);
            }
        } catch (final ParseException e) {
            // Une exception est levee lorsque le message n'indique pas la
            // taille
            // d'un fichier. Dans ce cas on ne fait rien de particulier car ce
            // cas
            // est pris en compte par le retour d'une valeur null.
        }

        return size;
    }

    /**
     * verifie si le message est du type 'Communication en cours avec le STAF'
     *
     * @param pResponse
     * @return pResponse si le message est bien du bon type, null sinon
     */
    protected String checkOngoingCommunicationMessage(String pResponse) {
        String messageString = null;
        final MessageFormat onGoingCommFormat = new MessageFormat(STAF_ONGOING_COMM_MESSAGE);
        try {
            onGoingCommFormat.parse(pResponse);
            messageString = pResponse;
        } catch (final ParseException e) {
            // Une exception est levee lorsque le message ne correspond pas a un
            // succes. Dans ce cas on ne fait rien de particulier car ce cas est
            // pris
            // en compte par le retour d'une chaine null.
        }

        return messageString;
    }

    /**
     * Vide le flux de sortie du STAF.
     *
     * @throws STAFException
     *             Erreur de lecture du flux de sortie du shell de pilotage
     */
    private void emptySTAFOutput() throws STAFException {
        try {
            if (stafOutput != null) {
                while (stafOutput.ready()) {
                    stafOutput.readLine();
                }
            }
        } catch (final IOException e) {
            // Erreur de lecture des reponses du STAF
            final String msg = "STAF response can not be read";
            logger.error(msg, e);
            throw new STAFException(msg, e);
        }
    }

    /**
     * Contrôle le format d'un nom de fichier (avec chemin d'acces). Les verifications suivantes sont effectuees :
     * <ul>
     * <li>Commence par le caractere /</li>
     * <li>Chaque noeud contient moins de 16 caracteres</li>
     * <li>Le nom du fichier fait moins de 64 caracteres</li>
     * <li>Les caracteres interdits sont ,;-</li>
     * </ul>
     *
     * @param pFilename
     *            Nom du fichier a contrôler
     * @return Indique si le format du parametre est valide
     */
    private boolean checkLongFilename(String pFilename) {
        boolean valid = pFilename.startsWith(FILE_SEPARATOR);
        // Pour chaque partie du nom du fichier on effectue un controle de
        // validite
        final StringTokenizer tokenizer = new StringTokenizer(pFilename, FILE_SEPARATOR);
        while (valid && tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            // Controle de validite sur un noeud
            if (tokenizer.hasMoreTokens()) {
                valid = checkNodename(token);
            }
            // Controle de validite sur le nom du fichier
            else {
                valid = checkShortFilename(token);
            }
        }
        return valid;
    }

    /**
     * Contrôle le format d'un nom de fichier (sans chemin d'acces). Les verifications suivantes sont effectuees :
     * <ul>
     * <li>Le nom du fichier fait moins de 64 caracteres</li>
     * <li>Les caracteres interdits sont ,;-/</li>
     * </ul>
     *
     * @param pFilename
     *            Nom du fichier a contrôler
     * @return Indique si le format du parametre est valide
     */
    private boolean checkShortFilename(String pFilename) {
        return (pFilename != null) && (pFilename.length() <= FILE_MAX_LENGTH)
                && Pattern.matches(FILE_PATTERN, pFilename);
    }

    /**
     * Contrôle le format d'un nom de noeud (sans chemin d'acces). Les verifications suivantes sont effectuees :
     * <ul>
     * <li>Le nom du noeud fait moins de 16 caracteres</li>
     * <li>Les caracteres interdits sont ,;-/</li>
     * </ul>
     *
     * @param pNodename
     *            Nom du noeud a contrôler
     * @return Indique si le format du parametre est valide
     */
    private boolean checkNodename(String pNodename) {
        return (pNodename != null) && (pNodename.length() <= NODE_MAX_LENGTH)
                && Pattern.matches(FILE_PATTERN, pNodename);
    }

    /**
     * Contrôle le format d'un nom de projet STAF. Il s'agit d'un parametre alphanumerique d'au plus 32 caracteres.
     *
     * @param pProject
     *            Nom du projet a contrôler
     * @return Indique si le format du parametre est valide
     */
    protected boolean checkProject(String pProject) {
        return (pProject != null) && (pProject.length() <= PROJECT_MAX_LENGTH) && checkAlphaNum(pProject);
    }

    /**
     * Contrôle le format d'un mot de passe STAF. Il s'agit d'un parametre alphanumerique d'au moins 6 caracteres et
     * d'au plus 16 caracteres. Il comporte obligatoirement au moins 2 caracteres numeriques.
     *
     * @param pPassword
     *            Mot de passe a contrôler
     * @return Indique si le format du parametre est valide
     */
    protected boolean checkPassword(String pPassword) {
        boolean valid = true;

        if ((pPassword == null) || (pPassword.length() > PASSWORD_MAX_LENGTH)
                || (pPassword.length() < PASSWORD_MIN_LENGTH) || !Pattern.matches(PASSWORD_PATTERN, pPassword)) {
            valid = false;
        }

        return valid;
    }

    /**
     * Contrôle le format d'un parametre alphanumerique. Seuls les caracteres a-zA-Z0-9._ sont autorises.
     *
     * @param pValue
     *            Valeur du parametre a contrôler
     * @return Indique si le format du parametre est valide
     */
    private boolean checkAlphaNum(String pValue) {
        boolean valid = true;

        if ((pValue == null) || !Pattern.matches(ALPHANUM_PATTERN, pValue)) {
            valid = false;
        }

        return valid;
    }
}
