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
package fr.cnes.regards.modules.acquisition.domain.chain;

public class AcquisitionProcessingChainMonitor {

    private AcquisitionProcessingChain chain;

    @SuppressWarnings("unused")
    private final Long chainId;

    // FILES
    private Long nbFileErrors = 0L;

    private Long nbFiles = 0L;

    private Long nbFilesInProgress = 0L;

    // PRODUCTS
    private Long nbProductErrors = 0L;

    private Long nbProducts = 0L;

    private Long nbProductsInProgress = 0L;

    // JOBS
    private long nbProductAcquisitionJob = 0;

    private long nbSIPGenerationJobs = 0;

    private long nbSIPSubmissionJobs = 0;

    private boolean active = false;

    // Post processing jobs not managed here ... can be seen in product

    public AcquisitionProcessingChainMonitor(AcquisitionProcessingChain chain) {
        super();
        this.chain = chain;
        this.chainId = chain.getId();
    }

    public AcquisitionProcessingChain getChain() {
        return chain;
    }

    public void setChain(AcquisitionProcessingChain chain) {
        this.chain = chain;
    }

    public Long getNbProductErrors() {
        return nbProductErrors;
    }

    public void setNbProductErrors(Long nbProductErrors) {
        this.nbProductErrors = nbProductErrors;
    }

    public Long getNbProducts() {
        return nbProducts;
    }

    public void setNbProducts(Long nbProducts) {
        this.nbProducts = nbProducts;
    }

    public Long getNbFileErrors() {
        return nbFileErrors;
    }

    public void setNbFileErrors(Long nbFileErrors) {
        this.nbFileErrors = nbFileErrors;
    }

    public Long getNbFiles() {
        return nbFiles;
    }

    public void setNbFiles(Long nbFiles) {
        this.nbFiles = nbFiles;
    }

    public Long getNbFilesInProgress() {
        return nbFilesInProgress;
    }

    public void setNbFilesInProgress(Long nbFilesInProgress) {
        this.nbFilesInProgress = nbFilesInProgress;
    }

    public Long getNbProductsInProgress() {
        return nbProductsInProgress;
    }

    public void setNbProductsInProgress(Long nbProductsInProgress) {
        this.nbProductsInProgress = nbProductsInProgress;
    }

    public long getNbProductAcquisitionJob() {
        return nbProductAcquisitionJob;
    }

    public void setNbProductAcquisitionJob(long nbProductAcquisitionJob) {
        this.nbProductAcquisitionJob = nbProductAcquisitionJob;
    }

    public long getNbSIPGenerationJobs() {
        return nbSIPGenerationJobs;
    }

    public void setNbSIPGenerationJobs(long nbSIPGenerationJobs) {
        this.nbSIPGenerationJobs = nbSIPGenerationJobs;
    }

    public long getNbSIPSubmissionJobs() {
        return nbSIPSubmissionJobs;
    }

    public void setNbSIPSubmissionJobs(long nbSIPSubmissionJobs) {
        this.nbSIPSubmissionJobs = nbSIPSubmissionJobs;
    }

    public boolean isActive() {
        active = (nbProductAcquisitionJob > 0) || (nbSIPGenerationJobs > 0) || (nbSIPSubmissionJobs > 0);
        return active;
    }
}
