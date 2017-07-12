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
package fr.cnes.regards.framework.modules.jobs.dao.stubs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameters;
import fr.cnes.regards.framework.modules.jobs.domain.JobParametersFactory;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.Output;
import fr.cnes.regards.framework.test.repository.RepositoryStub;

/***
 * {@link PluginConfiguration} Repository stub.
 *
 * @author Christophe Mertz
 *
 */
@Repository
@Primary
@Profile("test")
public class JobInfoRepositoryStub extends RepositoryStub<JobInfo> implements IJobInfoRepository {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoRepositoryStub.class);

    public JobInfoRepositoryStub() {
        try {
            jobInfo1.getStatus().setDescription("status jobInfo1");

            List<Output> outputs = new ArrayList<>();
            outputs.add(new Output(MediaType.APPLICATION_JSON_UTF8_VALUE, new URI("http://localhost/80/file.txt")));
            outputs.add(new Output(MediaType.APPLICATION_OCTET_STREAM_VALUE, new URI("http://localhost/results.txt")));

            jobInfo1.setResult(outputs);
            setIdAndStatusAndAddToRepository(jobInfo1, 33L, JobStatus.RUNNING);

            jobInfo2.getStatus().setDescription("status jobInfo2 azertyazerty");
            setIdAndStatusAndAddToRepository(jobInfo2, 44L, JobStatus.QUEUED);

            jobInfo3.getStatus().setDescription("status jobInfo3 azertyazerty");
            setIdAndStatusAndAddToRepository(jobInfo3, 55L, JobStatus.RUNNING);

        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void setIdAndStatusAndAddToRepository(JobInfo pJobInfo, Long pId, JobStatus pStatus) {
        pJobInfo.setId(pId);
        pJobInfo.getStatus().setJobStatus(pStatus);
        getEntities().add(pJobInfo);
    }

    @Override
    public List<JobInfo> findAllByStatusStatus(JobStatus pStatus) {
        try (Stream<JobInfo> stream = getEntities().stream()) {
            final List<JobInfo> jobs = new ArrayList<>();
            stream.filter(j -> j.getStatus().getJobStatus().equals(pStatus)).forEach(j -> jobs.add(j));
            return jobs;
        }
    }

    private static final JobParameters jobParameters1 = JobParametersFactory.build().addParameter("param11", "value11")
            .addParameter("param12", "value12").addParameter("param13", "value13").getParameters();

    private final static Path workspace = FileSystems.getDefault().getPath("/home", "cmertz", "git-regards",
                                                                           "rs-microservice");

    private static JobConfiguration jobConfiguration1 = new JobConfiguration("", jobParameters1,
            "fr.cnes.regards.modules.MyCustomJob", OffsetDateTime.now().plusDays(2), OffsetDateTime.now().plusDays(15),
            33, workspace, "owner@unix.org");

    private static final JobInfo jobInfo1 = new JobInfo(jobConfiguration1);

    private static final JobParameters jobParameters2 = JobParametersFactory.build()
            .addParameter("parameter21", "parameter value21").addParameter("parameter22", "parameter value22")
            .addParameter("parameter23", "parameter value23").addParameter("parameter24", "parameter value24")
            .getParameters();

    private static JobConfiguration jobConfiguration2 = new JobConfiguration("", jobParameters2,
            "fr.cnes.regards.modules.MyOtherCustomJob", OffsetDateTime.now().plusDays(2),
            OffsetDateTime.now().plusDays(15), 10, null, "master@ubuntu.org");

    private static final JobInfo jobInfo2 = new JobInfo(jobConfiguration2);

    private static final JobParameters jobParameters3 = JobParametersFactory.build()
            .addParameter("parameter31", "parameter value31").addParameter("parameter32", "parameter value32")
            .addParameter("parameter33", "parameter value33").addParameter("parameter34", "parameter value34")
            .addParameter("parameter35", "parameter value35").getParameters();

    private static JobConfiguration jobConfiguration3 = new JobConfiguration("", jobParameters3,
            "fr.cnes.regards.modules.MyOtherCustomJob", OffsetDateTime.now().plusDays(2),
            OffsetDateTime.now().plusDays(15), 10, null, "master@ubuntu.org");

    private static final JobInfo jobInfo3 = new JobInfo(jobConfiguration3);

}
