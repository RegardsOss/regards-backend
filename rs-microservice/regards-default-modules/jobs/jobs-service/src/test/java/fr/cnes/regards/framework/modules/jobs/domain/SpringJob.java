package fr.cnes.regards.framework.modules.jobs.domain;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;

/**
 * @author oroussel
 */
public class SpringJob extends AbstractNoParamJob<Void> {
    @Autowired
    private IJobInfoRepository repository;

    @Override
    public void run() {
        repository.findAll();
    }
}
