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

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

public class AcquisitionProcessingChainMonitor {

    private AcquisitionProcessingChain chain;

    private final Long chainId;

    private Long nbProductErrors = 0L;

    private Long nbProducts = 0L;

    private Long nbFileErrors = 0L;

    private Long nbFiles = 0L;

    private Long nbFilesInProgress = 0L;

    private Long nbProductsInProgress = 0L;

    private JobInfo scanJob = null;

    private Set<JobInfo> productGenerationJobs = Sets.newHashSet();

    private Set<JobInfo> sipSubmissionJobs = Sets.newHashSet();

    private Set<JobInfo> postProcessingJobs = Sets.newHashSet();

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

    public JobInfo getScanJob() {
        return scanJob;
    }

    public void setScanJob(JobInfo scanJob) {
        this.scanJob = scanJob;
    }

    public Set<JobInfo> getProductGenerationJobs() {
        return productGenerationJobs;
    }

    public void setProductGenerationJobs(Set<JobInfo> productGenerationJobs) {
        this.productGenerationJobs = productGenerationJobs;
    }

    public Set<JobInfo> getSipSubmissionJobs() {
        return sipSubmissionJobs;
    }

    public void setSipSubmissionJobs(Set<JobInfo> sipSubmissionJobs) {
        this.sipSubmissionJobs = sipSubmissionJobs;
    }

    public Set<JobInfo> getPostProcessingJobs() {
        return postProcessingJobs;
    }

    public void setPostProcessingJobs(Set<JobInfo> postProcessingJobs) {
        this.postProcessingJobs = postProcessingJobs;
    }

}
