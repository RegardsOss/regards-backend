package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;

/**
 * Cette classe regroupe les methodes communes aux etapes d'acquisition concernant la recupertation des donnees a
 * acquerir : </br>
 * AcquisitionScanStep - AcquisitionTransferRemoteFilesStep
 */

public abstract class AbstractAcquisitionScanPlugin {

    /**
     * Transforme le pattern indique dans la fourniture en pattern Java.<br>
     * Les correspondances implementees sont presentees dans le tableau suivant.<br>
     * Il est primordial de ne pas modifier l'ordre de prise en compte.<br>
     * <table border=1 cellpadding=2>
     * <tr>
     * <th>Ordre de prise en compte</th>
     * <th>Original</th>
     * <th>Adapte</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>.</td>
     * <td>\.</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>*</td>
     * <td>.*</td>
     * </tr>
     * </table>
     * 
     * @param originalPattern
     * @return
     */
    protected String getAdaptedPattern(String originalPattern) {

        String adaptedPattern = originalPattern;
        // "." => "\."
        adaptedPattern = replacePattern("\\.", "\\\\.", adaptedPattern);
        // "*" => ".*"
        adaptedPattern = replacePattern("\\*", "\\.\\*", adaptedPattern);
        return adaptedPattern;
    }

    /**
     * Remplace la chaine "pattern" par la chaine "replacement" dans "result"
     * 
     * @param strPattern
     * @param replacement
     * @param result
     * @return
     */
    protected String replacePattern(String strPattern, String replacement, String result) {

        Pattern pattern = Pattern.compile(strPattern);
        Matcher matcher = pattern.matcher(result);
        return matcher.replaceAll(replacement);
    }

    /**
     * Liste les fichiers dont le nom correspond au filtre et dont la date de derniere modification est posterieure a la
     * date de derniere acquisition
     *
     * @param dirFile
     *            Le repertoire contenant les fichiers
     * @param filter
     *            Le filtre pour les noms de fichiers
     * @param lastAcqDate
     *            La date de derniere acquisition
     * @return List<File> La liste des fichiers correspondant aux filtres
     */
    protected List<File> filteredFileList(File dirFile, RegexFilenameFilter filter, long lastAcqDate) {

        // Look for files with right pattern
        File[] nameFileArray = dirFile.listFiles(filter);
        List<File> sortedFileList = new ArrayList<>(nameFileArray.length);
        for (File element : nameFileArray) {
            long lasModifiedDate = element.lastModified();
            if (lasModifiedDate > lastAcqDate) {
                sortedFileList.add(element);
            } else {
                //                String msg = AcquisitionMessages.getInstance()
                //                        .getMessage("ssalto.service.acquisition.run.step.scan.lastAcqDate", element.getName());
                //                process_.addEventToReport(msg);
            }
        }
        return sortedFileList;
    }

    protected AcquisitionFile initAcquisitionFile(MetaFile metaFile, File baseFile, MetaProductDto metaProduct) {
        AcquisitionFile acqFile = new AcquisitionFile();
        acqFile.setMetaFile(metaFile);
        acqFile.setStatus(AcquisitionFileStatus.IN_PROGRESS);
        acqFile.setFileName(baseFile.getName());
        acqFile.setSize(new Long(baseFile.length()));
        return acqFile;
    }

}
