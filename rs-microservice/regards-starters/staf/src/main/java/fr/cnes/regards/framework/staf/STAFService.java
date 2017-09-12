package fr.cnes.regards.framework.staf;

/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 5.6 : FA : SIPNG-FA-1177-CN : 05/01/2015 : Correction archivage au STAF avec des caracteres invalides
 * VERSION : 5.5 : DM : SIPNG-DM-142-CN : 12/09/2014 : Interface CDPP/AMDA
 * VERSION : 5.3.1 : FA : SIPNG-FA-0953-CN : 31/10/2013 : blocage restauration STAF en cas de commandes simultanées utilisant toutes les sessions
 * VERSION : 5.3.1 : FA : SIPNG-FA-0945-CN : 21/10/2013 : blocage restauration STAF; mauvais nommage batchSessionsIt
 * VERSION : 5.3 : FA : SIPNG-FA-0932-CN : 24/07/2013 : typage de l'attribut files_
 *
 * VERSION : 2011/09/05 : 5.0 : CS
 * DM-ID : SIPNG-DM-0099-CN : 2011/09/05 : Conversion en UTF-8
 *
 * VERSION : 2011/05/05 : 4.7 : CS
 * DM-ID : SIPNG-DM-0071-CN : 2011/05/05 : ajout de getStatistics.
 *
 * VERSION : 2009/06/11 : 4.4 : CS
 * FA-ID : V44-FA-VR-FC-SSALTO-ARCH-010-01 : 2010/01/20 : commande de fichiers decoupes
 * FA-ID : V44-FA-VR-FC-CMDS-200-02 : 2010/01/06 : correction du nom du fichier STAF
 * DM-ID : SIPNG-DM-0035-CN : 2009/06/11 : ajout de collectListener_
 * FA-ID : SIPNG-FA-0476-CN : 2009/05/26 : la liste de fichiers a supprimer devient une Collection
 *
 * VERSION : 2009/01/15 : 4.3 : CS
 * DM-ID : SIPNG-DM-0049-CN : 29/12/2008 : Adaptation du code a la montee de version du staf Gerer plusieurs versions
 * star Passer qqes constantes a protected Passer qqes methodes a protected
 *
 * VERSION : 2008/10/31 : 4.2 : CS
 * FA-ID : SIPNG-FA-0351-CN : 2008/10/02 : bouclage archivage
 * DM-ID : SIPNG-DM-0044-2-CN : 2008/09/05 : ajout de deletFiles et modif de archiveFiles
 *
 * VERSION : 2008/06/01 : 4.1 : CS
 * DM-ID : SIPNG-DM-0044-CN : 05/03/2008 : Module d acquisistion et d archivage
 *
 * VERSION : 2007/11/16 : 4.0 : CS
 * FA-ID : SIPNG-FA-0272-CN : 2007/11/16 : ajout d'informations de log dans les threads
 *
 * VERSION : 2007/05/03 : 3.3 : CS
 * FA-ID : SIPNG-FA-0247-CS : 2007/07/03 : correction pour eviter de retenir une connexion ouverte quand une erreur
 * survient lors de la connexion.
 * DM-ID : SIPNG-DM-0032-CN : 2007/05/03 : ajout de setConfiguration dans l'interface
 *
 * VERSION : 2006/10/05 : 3.2 : CS
 * FA-ID : SIPNG-FA-0190-CS : 2006/10/05 : correction de restoreAllFiles pour ne pas recuperer les fichiers existant en
 * local
 *
 * VERSION : 2006/03/23 : 3.0 : CS
 * FA-ID : SIPNG-FA-0136-CS : 2006/04/12 : Correction javadoc
 * DM-ID : SIPNG-DM-0004-CN : 2006/03/23 : ajout de restoreDirectory
 *
 * VERSION : 2005/07/05 : 2.0 : CS
 * DM-ID : COMPLEMENT_V2 : 2005/11/14 : Correction des commentaire + ajout de logs
 * FA-ID : SIPNG-FA-0001-CS : 2005/10/24 : Prise en compte des remarques qualites
 * DM-ID : COMPLEMENT_V2 : 05/07/2005 : Conception detaillee du STAF Implementation des methodes de
 * l'interface. Remplacement des attributs existants par de nouveaux attributs
 * correspondant aux elements de conceptions lies a l'introduction de la classe
 * STAFSession.
 *
 * VERSION : 2004/06/02 : 1.0 : CS Creation
 *
 * FIN-HISTORIQUE
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.event.ICollectListener;

/**
 * Cette classe permet d'acceder aux services proposes par le STAF. Les fonctions utilisees sont celles de :
 * <ul>
 * <li>Restitution d'un ou plusieurs fichiers</li>
 * <li>Consultation des attributs d'un fichier</li>
 * <li>Verification de l'existence d'un fichier</li>
 * <li>Modification du nom d'un fichier</li>
 * </ul>
 * L'optimisation des restitutions est prise en charge par cette classe qui s'appuie sur les mecanismes de reservation
 * de session proposees par la classe <code>STAFManager</code> et sur les parametres de configuration du service (cf.
 * classe <code>STAFConfiguration</code>). Cette optimisation des restitutions consiste a calculer le nombre de sessions
 * a ouvrir pour prendre en charge chaque restitution et a repartir les fichiers sur chaque session. La distribution
 * recherchee permet de garantir un nombre minimal de commandes et une parallelisation maximale. <strong>Important
 * !</strong> Cette "optimisation" ne porte que sur les fichiers d'une seule commande. Si plusieurs commandes demandent
 * a restaurer les memes fichiers, les ordres de restauration au STAF seront "dupliques" entre chaque commande.
 */

public class STAFService {

    /**
     * Attribut permettant la journalisation
     *
     * @since 2.0
     */
    private static Logger logger = Logger.getLogger(STAFService.class);

    /**
     * Gestionnaire centrale de connexions au STAF.
     */
    private final STAFManager stafManager;

    /**
     * Session principale du service
     */
    protected STAFSession mainSession;

    /**
     * Identifiant de la session principale du service
     */
    protected Integer mainSessionId;

    /**
     * Maximum size (in Mo) for service classe : 50Mo
     *
     * @since 4.1
     */
    private static int CLASS_SERVICE_MAX_SIZE = 50000000;

    /**
     * Listener utilise des qu'un fichier eest collecte
     */
    private ICollectListener collectListener;

    /**
     * Archive STAF sur laquelle le service opère.
     */
    private final STAFArchive stafArchive;

    /**
     * Constructeur.
     */
    public STAFService(STAFManager pStafManager, STAFArchive pStafArchive) {
        mainSession = pStafManager.getNewSession();
        stafManager = pStafManager;
        stafArchive = pStafArchive;
    }

    /**
     * Cette methode permet de se connecter au systeme d'archivage.
     *
     * @param pProjectName
     *            : Nom du projet STAF
     * @param pMode
     *            : Mode d archivage ou de restitution
     * @throws STAFException
     */
    public void connectArchiveSystem(ArchiveAccessModeEnum pMode) throws STAFException {
        // Recupere une reference locale au gestionnaire STAF et a la
        // configuration
        logger.debug("Connecting to STAF ...");
        try {
            // Memorise la valeur des parametres de connexion pour permettre
            // l'ouverture ulterieure de sessions supplementaires
            // Reserve une session et ouvre la connexion selon le mode :
            // restitution ou archivage
            mainSessionId = stafManager.getReservation(pMode);
            mainSession.stafconOpen(stafArchive.getArchiveName(), stafArchive.getPassword());
            logger.debug("Connection to STAF Ok.");
        } catch (final STAFException e) {
            logger.error(e);

            try {
                // SIPNG-FA-0247-CS
                // we try to close connection, if any
                disconnectArchiveSystem(pMode);
            } catch (final STAFException e1) {
                // do nothing , closing the connection here must not log error
            }

            throw new STAFException(e);
        }
    }

    /**
     * Cette methode permet de se deconnecter du systeme d'archivage.
     *
     * @param pMode
     *            : Mode d archivage ou de restitution
     * @throws ArchiveException
     */
    public void disconnectArchiveSystem(ArchiveAccessModeEnum pMode) throws STAFException {
        // Ferme la connexion et libere la reservation
        mainSession.stafconClose();
        stafManager.freeReservation(mainSessionId, pMode);
    }

    /**
     * Permet de demander au service d'archivage la restitution d'un fichier
     *
     * @param pFileName
     *            Fichier a restituer
     * @param pDestination
     *            Emplacement ou restituer le fichier
     * @throws ArchiveException
     * @since 1.0
     */
    public void restoreFile(IRestoreFile pFileName, String pDestination) throws STAFException {
        // Prepare la liste des fichiers a restituer, liste ne contenant qu'un
        // seul fichier.
        final HashMap<String, String> files = new HashMap<>();
        files.put(pFileName.getPath(), computeTargetFilename(pFileName.getPath(), pDestination));
        // Pour la restitution d'un fichier unique on utilise la session
        // principale
        mainSession.staffilRetrieve(files);
    }

    /**
     * Permet de demander au service d'archivage la restitution d'un ensemble de fichiers. Contrairement aux autres
     * methode de l'interface STAF cette methode peut s'appuyer si necessaire sur plusieurs sessions
     *
     * @param pStafFilePathList
     *            : Liste de tous les fichiers a restaurer
     * @param pDestination
     *            : Emplacement ou restaurer le fichier
     * @throws ArchiveException
     */
    public void restoreAllFiles(Set<Path> pStafFilePathList, String pDestination) throws STAFException {

        STAFConfiguration conf = stafManager.getConfiguration();
        if ((pStafFilePathList != null) && !pStafFilePathList.isEmpty()) {
            // Iterateur sur les fichiers a restituer
            final Iterator<Path> files = pStafFilePathList.iterator();

            while (files.hasNext()) {

                // Nombre de sessions a ouvrir
                int sessionNb;
                // Tableau des identifiants de sessions a ouvrir
                List<Integer> sessionsIdentifiers = new ArrayList<>();
                // Index de parcours des sessions
                int sessionIndex;
                // Index de decompte des flots de fichiers par session
                int streamIndex;
                // Index de decompte des fichiers d'un flot
                int fileIndex;

                int maxFilesPerSession = conf.getMaxStreamFilesRestitutionMode()
                        * conf.getMaxSessionStreamsRestitutionMode();
                // Calcule le nombre de sessions a utiliser pour la restitution.
                // Ce nombre ne correspond pas directement au nombre de sessions
                // supplementaires a ouvrir puisque le service dispose d'une session dediee.
                sessionNb = (int) Math.ceil(pStafFilePathList.size() / (double) maxFilesPerSession);
                if (sessionNb > 0) {
                    sessionsIdentifiers = stafManager.getReservations(sessionNb - 1, false,
                                                                      ArchiveAccessModeEnum.RESTITUTION_MODE);
                    // Le nombre de sessions reellement alloue peut etre inferieur
                    // au nombre de sessions demandees
                    sessionNb = sessionsIdentifiers.size() + 1;
                } else {
                    // Seule la session principale est utile pour restituer les donnees
                    sessionNb = 1;
                }

                // Tableau des lots de fichiers par session
                final List<HashMap<String, String>> sessionsFiles = new ArrayList<>();
                // Initialise les lots de fichiers
                for (sessionIndex = 0; sessionIndex < sessionNb; sessionIndex++) {
                    final HashMap<String, String> indexHashMap = new HashMap<>();
                    sessionsFiles.add(indexHashMap);
                }

                // Repartit les fichiers sur les differents lots. Cette repartition a
                // pour objectif de distribuer le plus equitablement possible les
                // fichiers entre sessions. Toutefois on cherche aussi a limiter le
                // nombre de commandes STAF a passer. Ces deux remarques expliquent
                // le choix de l'algorithme suivant : On itere sur les donnees en
                // repartissant cycliquement sur chaque session un nombre de fichiers
                // egal au nombre maximum de fichiers par "flot" jusqu'a atteindre
                // pour chaque session le nombre maximum de "flots".
                streamIndex = 0;

                // SIPNG-DM-0035-CN : liste des fichiers traites
                final List<String> fileList = new ArrayList<>();
                final Set<Path> alreadyRestoredFile = Sets.newHashSet();

                while ((streamIndex < stafManager.getConfiguration().getMaxSessionStreamsRestitutionMode().intValue())
                        && files.hasNext()) {
                    sessionIndex = 0;
                    while ((sessionIndex < sessionNb) && files.hasNext()) {
                        // Pour ajouter des fichiers a la session courante on extrait
                        // la map qui stocke le lot de fichiers associe.
                        final HashMap<String, String> currentMap = sessionsFiles.get(sessionIndex);
                        fileIndex = 0;
                        while ((fileIndex < stafManager.getConfiguration().getMaxStreamFilesRestitutionMode()
                                .intValue()) && files.hasNext()) {
                            // Ajoute le prochain fichier au lot courant
                            final Path currentFile = files.next();
                            // si le fichier target existe,
                            // dans ce cas, ne pas le mettre dans la map
                            final String targetFileName = computeTargetFilename(currentFile.toString(), pDestination);
                            final File targetFile = new File(targetFileName);
                            if (!targetFile.exists()) {
                                currentMap.put(currentFile.toString(), targetFileName);
                            } else {
                                alreadyRestoredFile.add(currentFile);
                            }
                            // Add the file to the list
                            // event if the targetFile exists
                            fileList.add(currentFile.toString());

                            // On passe au fichier suivant
                            fileIndex++;
                        }
                        // On poursuit la distribution sur la session suivante
                        sessionIndex++;
                    }
                    // On poursuit la distribution avec un flot supplementaire
                    streamIndex++;
                }
                // Send an event to the collect listener
                if ((getCollectListener() != null) && !alreadyRestoredFile.isEmpty()) {
                    final CollectEvent collectEnd = new CollectEvent(this);
                    collectEnd.setRestoredFilePaths(alreadyRestoredFile);
                    getCollectListener().collectEnded(collectEnd);
                }
                // Lance la restitution des lots calcules
                runSessionsStaffilRetrieve(sessionsIdentifiers, sessionsFiles);
            }
        }
    }

    /**
     * Teste l'existance d'un fichier dans le systeme d'archivage.
     *
     * @param pFile
     *            : Fichier a tester
     * @return true si le fichier est deja present dans le systeme, false sinon.
     * @throws ArchiveException
     */
    public boolean fileExist(String pFile) throws STAFException {
        return mainSession.staffilExist(pFile);
    }

    /**
     * Permet de renommer un fichier dans le systeme d'archivage.
     *
     * @param pOldName
     *            : Ancien nom du fichier dans le systeme d'archivage
     * @param pNewName
     *            : Nouveau nom
     * @throws ArchiveException
     */
    public void renameFile(String pOldName, String pNewName) throws STAFException {
        mainSession.staffilModify(pOldName, pNewName);
    }

    /**
     * Permet de demander des information sur un fichier du systeme d'archivage.
     *
     * @param pFile
     *            : Fichier sur lequel on veut des informations
     * @return struture contenant les informations du fichier
     * @throws ArchiveException
     */
    public STAFFile getFileInfo(String pFile) throws STAFException {
        // Recupere les attributs du fichier
        final Map<String, Integer> attributes = mainSession.staffilList(pFile);
        // Construit la structure informative attendue
        final STAFFile info = new STAFFile();

        info.setFilePath(pFile);
        info.setFileSize(attributes.get(STAFSession.STAF_ATTRIBUTE_SIZE));

        return info;
    }

    /**
     * Calcule le nom d'un fichier une fois restitue du STAF. Ce nom est calcule sur la base du nom complet du fichier
     * au STAF et du nom du repertoire de destination du fichier.
     *
     * @param pFile
     *            Chemin d'acces complet au fichier a restituer
     * @param pDestination
     *            Repertoire de destination du fichier
     * @return Nom du fichier une fois restitue du STAF
     * @throws ArchiveException
     */
    private String computeTargetFilename(String pFile, String pDestination) {
        // Nom du fichier de destination
        String targetFileName = "";
        // Utilise un tokenizer pour rechercher le nom court du fichier
        final StringTokenizer tokenizer = new StringTokenizer(pFile, File.separator);
        while (tokenizer.hasMoreElements()) {
            targetFileName = (String) tokenizer.nextElement();
        }

        return pDestination + "/" + targetFileName;
    }

    /**
     * Lance la restitution de plusieurs lots de fichiers. Un des lots est assure par la session principale de
     * l'interface. Les autres (s'il y en a) sont assures par des sessions batch.
     *
     * @param pIdentifiers
     *            Liste des identifiants de session (hors session principale)
     * @param pSessionsFiles
     *            Liste des lots de fichiers a archiver. L'un de ces lots etant assume par la sessions principale du
     *            service, il y a un element dans cette liste que dans la liste des indentifiants de session.
     */
    private void runSessionsStaffilRetrieve(List<Integer> pIdentifiers, List<HashMap<String, String>> pSessionsFiles)
            throws STAFException {
        // Liste des sessions batchs lancees
        final ArrayList<STAFBackgroundSessionRetrieve> batchSessions = new ArrayList<>();
        // Indique si une erreur s'est produite sur l'une des sessions
        boolean error = false;
        // Cree les sessions batch
        final Iterator<Integer> batchSessionIt = pIdentifiers.iterator();
        while (batchSessionIt.hasNext()) {
            // Obtient un identifiant pour la session batch
            final Integer identifier = batchSessionIt.next();
            // Recupere un lot de fichiers pour la session batch
            final HashMap<String, String> files = pSessionsFiles.remove(0);
            // Cree puis execute la session batch
            final STAFBackgroundSessionRetrieve session = getBackgroundSessionRetrieve(identifier, stafArchive
                    .getArchiveName(), stafArchive.getPassword(), stafArchive.isGFAccount(), files);
            session.setParentStack(NDC.cloneStack());
            batchSessions.add(session);
            session.start();
        }

        try {
            // Lance la restitution du dernier lot via la session principale. La
            // restitution est bufferisee (par flots).
            final HashMap<String, String> files = pSessionsFiles.remove(0);
            mainSession.staffilRetrieveBuffered(files);
            // Send an event to the collect listener
            if (getCollectListener() != null) {
                final CollectEvent collectEnd = new CollectEvent(this);
                final Set<Path> filePaths = Sets.newHashSet();
                for (final String fileName : files.keySet()) {
                    filePaths.add(Paths.get(fileName));
                }
                collectEnd.setRestoredFilePaths(filePaths);
                getCollectListener().collectEnded(collectEnd);
            }
        } catch (final STAFException e) {
            logger.error(e.getMessage(), e);
            error = true;
        }

        // Attend la fin des sessions batch
        final Iterator<STAFBackgroundSessionRetrieve> batchSessionsIt = batchSessions.iterator();
        while (batchSessionsIt.hasNext()) {
            final STAFBackgroundSessionRetrieve session = batchSessionsIt.next();
            // Attend la fin de la session batch
            try {
                session.join();
            } catch (final InterruptedException e) {
                // Une interruption de l'attente n'est pas une erreur
            }
            // Verifie l'etat final de la session batch
            if (session.isInError()) {
                error = true;
            }
        }

        // Si une des sessions ne s'est pas terminee correctement on emet un
        // message d'erreur global a la restitution
        if (error) {
            final String msg = "Some files could not be restored";
            logger.error(msg);
            throw new STAFException(msg);
        }

    }

    /**
     * Lance l archivage de plusieurs lots de fichiers. Un des lots est assure par la session principale de l'interface.
     * Les autres (s'il y en a) sont assures par des sessions batch.
     *
     * @param pIdentifiers
     *            Liste des identifiants de session (hors session principale)
     * @param pFlowFiles
     *            Liste des flots de fichiers a archiver. L'un de ces lots etant assume par la sessions principale du
     *            service, il y a un element dans cette liste que dans la liste des indentifiants de session.
     * @param pDirectory
     *            Repertoire ou archiver au STAF
     * @return une HashMap contenant les repertoire reels d'archivage pour chaque fichier passe en entree.
     */
    private List<String> runSessionsStaffilArchive(List<Integer> pIdentifiers, List<STAFArchivingFlow> pFlowList,
            String pDirectory, boolean pReplace) throws STAFException {
        // Map to return :
        final List<String> archivedFilesList = new ArrayList<>();
        // List of batchs sessions already launched
        final List<STAFBackgroundSessionArchive> batchSessions = new ArrayList<>();
        // Inform if an error append on one of sessions
        boolean error = false;
        final int nbMaxFlowSession = stafManager.getConfiguration().getMaxSessionStreamsArchivingMode().intValue();

        // Create batch sessions
        final Iterator<Integer> batchSessionIt = pIdentifiers.iterator();
        while (batchSessionIt.hasNext()) {
            // Get identifier for the batch session
            final Integer identifier = batchSessionIt.next();
            // Flow list
            final List<STAFArchivingFlow> sessionFlowList = new ArrayList<>();

            for (int i = 0; i < nbMaxFlowSession; i++) {
                // Get all files for the batch session
                final STAFArchivingFlow flow = pFlowList.remove(0);
                sessionFlowList.add(flow);
            }
            // Create and execute the batch session
            final STAFBackgroundSessionArchive session = getBackgroundSessionArchive(identifier,
                                                                                     stafArchive.getArchiveName(),
                                                                                     stafArchive.getPassword(),
                                                                                     sessionFlowList, pDirectory,
                                                                                     archivedFilesList, pReplace);
            session.setParentStack(NDC.cloneStack());
            batchSessions.add(session);
            session.start();
        }

        try {
            // Launch archiving of the last lot via principal session.
            // Archiving is bufferised (by flow).
            final STAFArchivingFlow flow = pFlowList.remove(0);
            archivedFilesList.addAll(mainSession.staffilArchive(flow.getFilesMap(), flow.getServiceClass(), pReplace));
        } catch (final STAFException e) {
            logger.warn("", e);
            error = true;
        }

        // Wait the end of batch session
        final Iterator<STAFBackgroundSessionArchive> batchSessionsIt = batchSessions.iterator();
        while (batchSessionsIt.hasNext()) {
            final STAFBackgroundSessionArchive session = batchSessionsIt.next();
            // Wait the end of batch session
            try {
                session.join();

            } catch (final InterruptedException e) {
                // An waiting interruption is not an error
            }
            // Check final state of the batch session
            if (session.isInError()) {
                error = true;
            }
            // add the return files to the archivedFilesList
            archivedFilesList.addAll(session.getArchivedFilesList());
        }

        // If one of sessions is not correctly terminated, we send an global
        // error message to the archiving
        if (error) {
            final String msg = "Some files could not be archived";
            logger.error(msg);
            throw new STAFException(msg);
        }
        return archivedFilesList;
    }

    /**
     * cree la bonne session de background pour l'archivage
     *
     * @param pSessionId
     * @param pProject
     * @param pPassword
     * @param pGFAccount
     * @param pFilesFlow
     * @param pDirectory
     * @param pArchivedFilesList
     * @param pReplace
     * @return
     * @since 4.3
     * @DM SIPNG-DM-0049-CN : creation
     */
    protected STAFBackgroundSessionArchive getBackgroundSessionArchive(Integer pSessionId, String pProject,
            String pPassword, List<STAFArchivingFlow> pFilesFlow, String pDirectory, List<String> pArchivedFilesList,
            boolean pReplace) {
        final STAFBackgroundSessionArchive session = new STAFBackgroundSessionArchive(pSessionId, pProject, pPassword,
                pFilesFlow, pDirectory, pArchivedFilesList, pReplace, stafManager.getConfiguration());
        return session;
    }

    /**
     * cree la bonne session de background pour la commande
     *
     * @param pSessionId
     * @param pProject
     * @param pPassword
     * @param pGFAccount
     * @param pFiles
     * @return
     * @since 4.3 SIPNG-DM-0049-CN : creation
     */
    protected STAFBackgroundSessionRetrieve getBackgroundSessionRetrieve(Integer pSessionId, String pProject,
            String pPassword, boolean pGFAccount, HashMap<String, String> pFiles) {
        final STAFBackgroundSessionRetrieve session = new STAFBackgroundSessionRetrieve(pSessionId, pProject, pPassword,
                pFiles, stafManager.getConfiguration(), getCollectListener());
        return session;
    }

    /**
     * Cette methode permet d'archiver les fichiers dans un repertoire construit a partir du pDirectory
     *
     * @param pFileMap
     *            une HashMap qui contient en clef le path du fichier local, et en valeur le path du fichier distant.
     *            Cette map ne doit pas etre null
     * @param pDirectory
     *            le repertoire dans lequel ajouter les fichiers.
     * @return une HashMap contenant les repertoire reels d'archivage pour chaque fichier passe en entree.
     */
    public Set<String> archiveFiles(Map<String, String> pFileMap, String pDirectory, boolean pReplace)
            throws STAFException {

        // List of files really archived
        final Set<String> archivedFilesList = Sets.newHashSet();
        // Iterateur sur les fichiers a archiver
        final Set<String> fileLocalList = pFileMap.keySet();
        final Iterator<String> files = fileLocalList.iterator();

        // Map of files in function of Service Class (CS1, CS3, CS5)
        // Class service little files : size<=50Mo
        final Map<String, String> littleFileServiceClassMap = new HashMap<>();
        // Class service bigger files gen staf : size>50Mo [generalist STAF]
        final Map<String, String> biggerFileGenServiceClassMap = new HashMap<>();
        // Class service bigger files GF staf : size>50Mo [GF Account STAF (big
        // file)]
        final Map<String, String> biggerFileGFServiceClassMap = new HashMap<>();

        // Dispacth all files in function Service Class in 3 Map
        while (files.hasNext()) {
            // Current file
            final String currentFile = files.next();
            final String destinationCurrentFile = pFileMap.get(currentFile);
            final File currentFilePath = new File(currentFile);

            // Compare current file size with limit file size to dispatch file
            // in appropriated class service
            if (currentFilePath.exists() && currentFilePath.isFile()) {
                // Current file size
                final long fileSize = currentFilePath.length();

                // Class service CS1 : size<=50Mo
                // ******************************
                if (fileSize <= CLASS_SERVICE_MAX_SIZE) {
                    littleFileServiceClassMap.put(currentFile, destinationCurrentFile);
                }
                // Class service CS3 or CS5 : size>50Mo
                else {
                    // CS5 : GF Account STAF (big file) : size>50Mo
                    // ********************************************
                    if (stafArchive.isGFAccount()) {
                        biggerFileGFServiceClassMap.put(currentFile, destinationCurrentFile);
                    }
                    // CS3 : generalist STAF : size>50Mo
                    // *********************************
                    else {
                        biggerFileGenServiceClassMap.put(currentFile, destinationCurrentFile);
                    }
                }
            }
        }

        // For each Map of Class Service, dispach file in several flow
        final List<STAFArchivingFlow> flowList = new ArrayList<>();
        final int maxStreamFiles = stafManager.getConfiguration().getMaxStreamFilesArchivingMode().intValue();
        if (stafManager.getConfiguration().getLittleFileClass() == null) {
            logger.error("Service class 1 is not set in configuration, CS1 class is used as default");
            flowList.addAll(dispatchFilesInSeveralFlow(littleFileServiceClassMap, maxStreamFiles, "CS1"));
        } else {
            flowList.addAll(dispatchFilesInSeveralFlow(littleFileServiceClassMap, maxStreamFiles,
                                                       stafManager.getConfiguration().getLittleFileClass()));
        }
        if (stafManager.getConfiguration().getBiggerFileGenClass() == null) {
            logger.error("Service class 2 is not set in configuration, CS3 class is used as default");
            flowList.addAll(dispatchFilesInSeveralFlow(biggerFileGenServiceClassMap, maxStreamFiles, "CS3"));
        } else {
            flowList.addAll(dispatchFilesInSeveralFlow(biggerFileGenServiceClassMap, maxStreamFiles,
                                                       stafManager.getConfiguration().getBiggerFileGenClass()));
        }
        if (stafManager.getConfiguration().getBiggerFileGFClass() == null) {
            logger.error("Service class 3 is not set in configuration, CS5 class is used as default");
            flowList.addAll(dispatchFilesInSeveralFlow(biggerFileGFServiceClassMap, maxStreamFiles, "CS5"));
        } else {
            flowList.addAll(dispatchFilesInSeveralFlow(biggerFileGFServiceClassMap, maxStreamFiles,
                                                       stafManager.getConfiguration().getBiggerFileGFClass()));
        }

        // Total flow
        final int totalFlow = flowList.size();
        // Table of Sessions identifiers to open
        List<Integer> sessionsIdentifiers = new ArrayList<>();

        final int maxSessionsStream = stafManager.getConfiguration().getMaxSessionStreamsArchivingMode().intValue();

        final Iterator<STAFArchivingFlow> iter = flowList.iterator();
        while (iter.hasNext()) {
            // Number of sessions to open
            int sessionNbNeed = totalFlow / maxSessionsStream;
            if (sessionNbNeed > 0) {
                sessionsIdentifiers = stafManager.getReservations(sessionNbNeed - 1, false,
                                                                  ArchiveAccessModeEnum.ARCHIVE_MODE);
            }

            // Launch the archiving of computed lots
            archivedFilesList.addAll(runSessionsStaffilArchive(sessionsIdentifiers, flowList, pDirectory, pReplace));
        }
        return archivedFilesList;
    }

    public List<File> deleteFiles(Set<String> pFileList) throws STAFException {
        final List<String> filePaths = mainSession.staffilDelete(pFileList);
        final List<File> files = new ArrayList<>();
        if (filePaths != null) {
            for (final String path : filePaths) {
                files.add(new File(path));
            }
        }
        return files;
    }

    /**
     * Repartit les fichiers en plusieurs flots. On otint une liste de Map (dont la taille max est pMaxStreamFiles)
     *
     * @param pFilesMap
     *            : tous les fichiers à repartir (cle : emplcement source, emplacement destination)
     * @param pMaxStreamFiles
     *            : nombre maximum de fichiers par flots
     * @return une liste de MAP dont la taille max de chaque MAP est pMaxStreamFiles (List de Map)
     */
    public List<STAFArchivingFlow> dispatchFilesInSeveralFlow(Map<String, String> pFilesMap, int pMaxStreamFiles,
            String pServiceClass) {
        // Flow list to return
        final List<STAFArchivingFlow> flowList = new ArrayList<>();

        // Loop on pFilesMap
        final Iterator<String> iter = pFilesMap.keySet().iterator();
        STAFArchivingFlow flow = new STAFArchivingFlow();
        flow.setServiceClass(pServiceClass);

        while (iter.hasNext()) {
            // Get new source/destination
            final String source = iter.next();
            final String destination = pFilesMap.get(source);

            // Check flow size
            if (flow.getFilesMap().size() >= pMaxStreamFiles) {
                // Max flow size has been reached
                // Add STAF flow entry in the flowList
                flowList.add(flow);
                // Init a new flow
                flow = new STAFArchivingFlow();
                flow.setServiceClass(pServiceClass);
            }
            // Add source/destination in Map
            flow.addFileToFlow(source, destination);
        }

        // Flush MAP
        if (!flow.getFilesMap().isEmpty()) {
            flowList.add(flow);
        }
        // Return several flow of files
        return flowList;
    }

    public List<String> getStatistics() throws STAFException {
        return mainSession.stafstat();
    }

    public STAFArchive getStafArchive() {
        return stafArchive;
    }

    public ICollectListener getCollectListener() {
        return collectListener;
    }

    public void setCollectListener(ICollectListener pCollectListener) {
        collectListener = pCollectListener;
    }

}
