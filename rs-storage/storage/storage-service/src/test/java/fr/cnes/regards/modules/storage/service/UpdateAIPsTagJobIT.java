package fr.cnes.regards.modules.storage.service;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.UpdatedAipsInfos;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Léo Mieulet
 */
public class UpdateAIPsTagJobIT  extends AbstractJobIT {

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_420")
    public void testAddTags() throws InterruptedException {
        AddAIPTagsFilters filters = new AddAIPTagsFilters();
        Set<String> tags = new HashSet<String>();
        tags.add("first tag");
        tags.add("new tag");
        filters.setTagsToAdd(tags);
        filters.setSession(SESSION);

        // Create the job
        aipService.addTagsByQuery(filters);

        // Wait until the job finishes
        JobInfo jobInfo = waitForJobFinished();

        // Check the job is finished and has a result
        JobInfo jobInfoRefreshed = jobInfoRepo.findById(jobInfo.getId());
        Assert.assertEquals(jobInfoRefreshed.getStatus().getStatus(), JobStatus.SUCCEEDED);
        UpdatedAipsInfos result = jobInfoRefreshed.getResult();
        Assert.assertEquals("should not produce error", 0, result.getNbErrors());
        int nbUpdated = 20;
        Assert.assertEquals("should produce updated AIP", nbUpdated, result.getNbUpdated());
        Assert.assertEquals("AIP shall be tagged", nbUpdated, aipDao.findAllByTags("new tag").size());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_430")
    public void testDeleteTags() throws InterruptedException {
        RemoveAIPTagsFilters filters = new RemoveAIPTagsFilters();
        Set<String> tags = new HashSet<String>();
        tags.add("first tag");
        filters.setTagsToRemove(tags);
        filters.setSession(SESSION);

        // Create the job
        aipService.removeTagsByQuery(filters);
        JobInfo jobInfo = waitForJobFinished();

        // Check the job is finished and has a result
        JobInfo jobInfoRefreshed = jobInfoRepo.findById(jobInfo.getId());
        Assert.assertEquals(jobInfoRefreshed.getStatus().getStatus(), JobStatus.SUCCEEDED);
        UpdatedAipsInfos result = jobInfoRefreshed.getResult();
        Assert.assertEquals("should not produce error", 0, result.getNbErrors());
        int nbUpdated = 20;
        Assert.assertEquals("should produce updated AIP", nbUpdated, result.getNbUpdated());
        Assert.assertEquals("no more AIP shall be tagged with the tag", 0, aipDao.findAllByTags("first tag").size());
        Assert.assertEquals("AIP still have this tag", nbUpdated, aipDao.findAllByTags("second tag").size());
    }
}
