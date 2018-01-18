/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

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
import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.domain.STAFArchivingFlow;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.domain.STAFFile;
import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.exception.STAFException;

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
 * @author CS
 */

public class STAFService {

    /**
     * Class logger
     */
    private static final Logger logger = LoggerFactory.getLogger(STAFService.class);

    /**
     * Gestionnaire centrale de connexions au STAF.
     */
    private final STAFSessionManager stafManager;

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
     */
    private static int CLASS_SERVICE_MAX_SIZE = 50000000;

    /**
     * Listener utilise des qu'un fichier eest collecte
     */
    private STAFCollectListener collectListener;

    /**
     * Archive STAF sur laquelle le service opère.
     */
    private final STAFArchive stafArchive;

    /**
     * Constructeur.
     */
    public STAFService(STAFSessionManager pStafManager, STAFArchive pStafArchive) {
        mainSession = pStafManager.getNewSession();
        stafManager = pStafManager;
        stafArchive = pStafArchive;
    }

    /**
     * Cette methode permet de se connecter au systeme d'archivage.
     * @param pProjectName : Nom du projet STAF
     * @param pMode : Mode d archivage ou de restitution
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
            logger.error(e.getMessage(), e);

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
     * @param pMode : Mode d archivage ou de restitution
     */
    public void disconnectArchiveSystem(ArchiveAccessModeEnum pMode) throws STAFException {
        // Ferme la connexion et libere la reservation
        logger.debug("Disconnecting from STAF ...");
        mainSession.stafconClose();
        stafManager.freeReservation(mainSessionId, pMode);
        logger.debug("Disconnected from STAF.");
    }

    /**
     * Permet de demander au service d'archivage la restitution d'un fichier
     * @param pFileName Fichier a restituer
     * @param pDestination Emplacement ou restituer le fichier
     */
    public void restoreFile(Path pSTAFFilePath, Path pDestination) throws STAFException {
        // Prepare la liste des fichiers a restituer, liste ne contenant qu'un
        // seul fichier.
        final HashMap<String, String> files = new HashMap<>();
        files.put(pSTAFFilePath.toString(), computeTargetFilename(pSTAFFilePath.toString(), pDestination.toString()));
        // Pour la restitution d'un fichier unique on utilise la session
        // principale
        mainSession.staffilRetrieve(files);
    }

    /**
     * Permet de demander au service d'archivage la restitution d'un ensemble de fichiers. Contrairement aux autres
     * methode de l'interface STAF cette methode peut s'appuyer si necessaire sur plusieurs sessions
     * @param pStafFilePathList : Liste de tous les fichiers a restaurer
     * @param pDestination : Emplacement ou restaurer le fichier
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

                int maxFilesPerSession =
                        conf.getMaxStreamFilesRestitutionMode() * conf.getMaxSessionStreamsRestitutionMode();
                // Calcule le nombre de sessions a utiliser pour la restitution.
                // Ce nombre ne correspond pas directement au nombre de sessions
                // supplementaires a ouvrir puisque le service dispose d'une session dediee.
                sessionNb = (int) Math.ceil(pStafFilePathList.size() / (double) maxFilesPerSession);
                if (sessionNb > 0) {
                    sessionsIdentifiers = stafManager
                            .getReservations(sessionNb - 1, false, ArchiveAccessModeEnum.RESTITUTION_MODE);
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
     * @param pFile : Fichier a tester
     * @return true si le fichier est deja present dans le systeme, false sinon.
     */
    public boolean fileExist(String pFile) throws STAFException {
        return mainSession.staffilExist(pFile);
    }

    /**
     * Permet de renommer un fichier dans le systeme d'archivage.
     * @param pOldName : Ancien nom du fichier dans le systeme d'archivage
     * @param pNewName : Nouveau nom
     */
    public void renameFile(String pOldName, String pNewName) throws STAFException {
        mainSession.staffilModify(pOldName, pNewName);
    }

    /**
     * Permet de demander des information sur un fichier du systeme d'archivage.
     * @param pFile : Fichier sur lequel on veut des informations
     * @return struture contenant les informations du fichier
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
     * @param pFile Chemin d'acces complet au fichier a restituer
     * @param pDestination Repertoire de destination du fichier
     * @return Nom du fichier une fois restitue du STAF
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
     * @param pIdentifiers Liste des identifiants de session (hors session principale)
     * @param pSessionsFiles Liste des lots de fichiers a archiver. L'un de ces lots etant assume par la sessions principale du
     * service, il y a un element dans cette liste que dans la liste des indentifiants de session.
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
            final STAFBackgroundSessionRetrieve session = getBackgroundSessionRetrieve(identifier,
                                                                                       stafArchive.getArchiveName(),
                                                                                       stafArchive.getPassword(),
                                                                                       files);
            session.setParentStack(NDC.cloneStack());
            batchSessions.add(session);
            session.start();
        }

        final HashMap<String, String> files = pSessionsFiles.remove(0);
        try {
            // Lance la restitution du dernier lot via la session principale. La
            // restitution est bufferisee (par flots).
            mainSession.staffilRetrieveBuffered(files);
        } catch (final STAFException e) {
            logger.error(e.getMessage(), e);
            error = true;
        } finally {
            // Send an event to the collect listener
            if (getCollectListener() != null) {
                final CollectEvent collectEnd = new CollectEvent(this);
                final Set<Path> filePaths = Sets.newHashSet();
                final Set<Path> errorFilePaths = Sets.newHashSet();
                for (final String fileName : files.keySet()) {
                    Path restoredFile = Paths.get(files.get(fileName));
                    if (restoredFile.toFile().exists()) {
                        filePaths.add(Paths.get(fileName));
                    } else {
                        errorFilePaths.add(Paths.get(fileName));
                    }
                }
                collectEnd.setRestoredFilePaths(filePaths);
                collectEnd.setNotRestoredFilePaths(errorFilePaths);
                getCollectListener().collectEnded(collectEnd);
            }
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
     * @param pIdentifiers Liste des identifiants de session (hors session principale)
     * @param pFlowFiles Liste des flots de fichiers a archiver. L'un de ces lots etant assume par la sessions principale du
     * service, il y a un element dans cette liste que dans la liste des indentifiants de session.
     * @param pDirectory Repertoire ou archiver au STAF
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
     */
    protected STAFBackgroundSessionArchive getBackgroundSessionArchive(Integer pSessionId, String pProject,
            String pPassword, List<STAFArchivingFlow> pFilesFlow, String pDirectory, List<String> pArchivedFilesList,
            boolean pReplace) {
        return new STAFBackgroundSessionArchive(pSessionId, pProject, pPassword, pFilesFlow, pDirectory,
                                                pArchivedFilesList, pReplace, stafManager.getConfiguration());
    }

    /**
     * cree la bonne session de background pour la commande
     */
    protected STAFBackgroundSessionRetrieve getBackgroundSessionRetrieve(Integer pSessionId, String pProject,
            String pPassword, HashMap<String, String> pFiles) {
        return new STAFBackgroundSessionRetrieve(pSessionId, pProject, pPassword, pFiles,
                                                 stafManager.getConfiguration(), getCollectListener());
    }

    /**
     * Cette methode permet d'archiver les fichiers dans un repertoire construit a partir du pDirectory
     * @param pFileMap une HashMap qui contient en clef le path du fichier local, et en valeur le path du fichier distant.
     * Cette map ne doit pas etre null
     * @param pDirectory le repertoire dans lequel ajouter les fichiers.
     * @return une liste contenant les repertoire reels d'archivage pour chaque fichier passe en entree.
     */
    public Set<String> archiveFiles(Map<Path, Path> pFileMap, Path pDirectory, boolean pReplace) throws STAFException {

        // List of files really archived
        final Set<String> archivedFilesList = Sets.newHashSet();
        // Iterateur sur les fichiers a archiver
        final Set<Path> fileLocalList = pFileMap.keySet();
        final Iterator<Path> files = fileLocalList.iterator();

        // Map of files in function of Service Class (CS1, CS3, CS5)
        // Class service little files : size<=50Mo
        final Map<String, String> littleFileServiceClassMap = new HashMap<>();
        // Class service bigger files gen staf : size>50Mo [generalist STAF]
        final Map<String, String> biggerFileGenServiceClassMap = new HashMap<>();

        // Dispacth all files in function Service Class in 3 Map
        while (files.hasNext()) {
            // Current file
            final Path currentFile = files.next();
            final Path destinationCurrentFile = pFileMap.get(currentFile);
            final File currentFilePath = currentFile.toFile();

            // Compare current file size with limit file size to dispatch file
            // in appropriated class service
            if (currentFilePath.exists() && currentFilePath.isFile()) {
                // Current file size
                final long fileSize = currentFilePath.length();

                // Class service CS1 : size<=50Mo
                // ******************************
                if (fileSize <= CLASS_SERVICE_MAX_SIZE) {
                    littleFileServiceClassMap.put(currentFile.toString(), destinationCurrentFile.toString());
                }
                // Class service CS3 or CS5 : size>50Mo
                else {
                    biggerFileGenServiceClassMap.put(currentFile.toString(), destinationCurrentFile.toString());
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
                sessionsIdentifiers = stafManager
                        .getReservations(sessionNbNeed - 1, false, ArchiveAccessModeEnum.ARCHIVE_MODE);
            }

            // Launch the archiving of computed lots
            archivedFilesList
                    .addAll(runSessionsStaffilArchive(sessionsIdentifiers, flowList, pDirectory.toString(), pReplace));
        }
        return archivedFilesList;
    }

    /**
     * delete ginve {@link Path} files into STAF system.
     * @param pFileList list {@link Path} of files to delete
     * @return list of {@link Path} not deleted files.
     */
    public Set<Path> deleteFiles(Set<Path> pFileList) throws STAFException {
        final List<String> filePaths = mainSession.staffilDelete(pFileList);
        final Set<Path> files = Sets.newHashSet();
        if (filePaths != null) {
            for (final String path : filePaths) {
                files.add(Paths.get(path));
            }
        }
        return files;
    }

    /**
     * Repartit les fichiers en plusieurs flots. On otint une liste de Map (dont la taille max est pMaxStreamFiles)
     * @param pFilesMap : tous les fichiers à repartir (cle : emplcement source, emplacement destination)
     * @param pMaxStreamFiles : nombre maximum de fichiers par flots
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

    public STAFCollectListener getCollectListener() {
        return collectListener;
    }

    public void setCollectListener(STAFCollectListener pCollectListener) {
        collectListener = pCollectListener;
    }

}
