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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.exception.CompressionException;

/**
 * 
 * Cette classe permet de definir les informations necessaires a la realisation de la compression. Elle possede une
 * donnee membre permettant de stocker la reference de la compression en cours (une instance de classe implementant
 * l'interface ICompression).
 * 
 * @author CS
 * @version $Revision: 1.13 $
 * @since 1.0
 */
public class CompressionContext {

    /**
     * Permet de stocker la ou les ressource(s) a compresser. La source peut etre un fichier ou un repertoire.
     * 
     * @since 1.0
     */
    List<File> inputSource_ = null;

    /**
     * Contient le fichier produit par la compression
     * 
     * @since 1.0
     */
    File compressedFile_ = null;

    /**
     * Contient le repertoire cible de decompression
     * 
     * @since 1.0
     */
    File outputDir_ = null;

    /**
     * Contient le répertoire racine des fichiers à compresser. Utile pour insérer les chemins des fichiers relatifs à
     * une racine (espace utilisateur dédié par exemple)
     * 
     * @since 1.0
     */
    File rootDirectory_ = null;

    /**
     * Designe la strategie de compression
     * 
     * @since 1.0
     */
    private ICompression referenceStrategy_;

    /**
     * indique si l'archive doit etre cree a plat
     * 
     * @since 4.4
     * @FA SIPNG-FA-0450-CN : creation
     */
    private boolean flatArchive_;

    /**
     * Format d'encodage des caracteres lors de la compression
     * 
     * @since 5.3
     * @DM SIPNG-DM-0121-CN
     */
    private Charset charSet_;

    /**
     * Lancement synchrone ou asynchrone de la compression
     * 
     * @since 5.4
     * @DM SIPNG-DM-0141-CN
     */
    private boolean runInThread_;

    /**
     * Constructeur de classe
     * 
     * @since 1.0
     */
    protected CompressionContext() {
    }

    /**
     * Cette methode declenche la compression
     * 
     * @exception CompressionException
     *                Exception survenue lors du traitement
     * @return le fichier compresse avec l'extension
     * @since 1.0
     * @FA SIPNG-FA-0450-CN : modification de code
     * 
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caractères
     */
    protected CompressManager doCompress() throws CompressionException {
        return referenceStrategy_.compress(inputSource_, compressedFile_, rootDirectory_, flatArchive_, runInThread_,
                                           charSet_);
    }

    /**
     * Cette methode declenche la decompression
     * 
     * @exception CompressionException
     *                Exception survenue lors du traitement.
     * @since 1.0
     * 
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caractères
     */
    protected void doUncompress() throws CompressionException {
        referenceStrategy_.uncompress(compressedFile_, outputDir_, charSet_);
    }

    /**
     * Definit le mode de compression
     * 
     * @param pCompressionMethod
     *            : le mode de compression
     * @since 1.0
     */
    protected void setCompression(ICompression pCompressionMethod) {
        referenceStrategy_ = pCompressionMethod;
    }

    /**
     * getter pour flatARchive
     * 
     * @since 4.4
     * @FA SIPNG-FA-0450-CN : creation
     */
    protected boolean isFlatArchive() {
        return flatArchive_;
    }

    // === setters === //

    /**
     * Modificateur
     * 
     * @param pFile
     * @since 1.0
     */
    public void setCompressedFile(File pFile) {
        compressedFile_ = pFile;
    }

    /**
     * Modificateur inputSource_
     * 
     * @param pList
     *            une liste de <code>File</code>
     * @since 1.0
     */
    public void setInputSource(List<File> pList) {
        inputSource_ = pList;
    }

    /**
     * Modificateur
     * 
     * @param pFile
     * @since 1.0
     */
    public void setOutputDir(File pFile) {
        outputDir_ = pFile;
    }

    /**
     * @return Returns the rootDirectory.
     */
    public File getRootDirectory() {
        return rootDirectory_;
    }

    /**
     * @param pRootDirectory
     *            The rootDirectory to set.
     */
    public void setRootDirectory(File pRootDirectory) {
        rootDirectory_ = pRootDirectory;
    }

    /**
     * setter pour fatARchive_
     * 
     * @since 4.4
     * @FA SIPNG-FA-0450-CN : creation
     */
    protected void setFlatArchive(boolean pFlatArchive) {
        flatArchive_ = pFlatArchive;
    }

    /**
     * getter charset
     * 
     * @since 5.3
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caractères
     */
    public Charset getCharSet() {
        return charSet_;
    }

    /**
     * setter charset
     * 
     * @since 5.3
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caractères
     */
    public void setCharSet(Charset charSet) {
        charSet_ = charSet;
    }

    /**
     * 
     * Getter runInThread_
     * 
     * @return
     * @since 5.4
     */
    public boolean isRunInThread() {
        return runInThread_;
    }

    /**
     * 
     * Setter runInThread_
     * 
     * @param pRunInThread
     * @since 5.4
     */
    public void setRunInThread(boolean pRunInThread) {
        runInThread_ = pRunInThread;
    }

}
