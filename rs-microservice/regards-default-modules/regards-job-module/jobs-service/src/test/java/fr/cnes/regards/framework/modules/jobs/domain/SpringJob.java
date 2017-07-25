package fr.cnes.regards.framework.modules.jobs.domain;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;

/**
 * @author oroussel
 */
public class SpringJob extends AbstractJob implements IJob {
    @Autowired
    private IJobInfoRepository repository;

    @Override
    public void run() {
        repository.findAll();
    }
}
