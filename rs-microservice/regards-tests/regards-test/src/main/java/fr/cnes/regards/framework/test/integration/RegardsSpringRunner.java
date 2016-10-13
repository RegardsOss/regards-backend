/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.test.report.RequirementMatrixReportListener;

/**
 * Custom spring runner to integrate custom reporter.<br/>
 * Allows to test listener locally.
 *
 * @author msordi
 *
 */
public final class RegardsSpringRunner extends SpringJUnit4ClassRunner {

    /**
     * @param pClazz
     * @throws InitializationError
     *             If runner can't be initialized
     */
    public RegardsSpringRunner(Class<?> pClazz) throws InitializationError {
        super(pClazz);
    }

    @Override
    public void run(RunNotifier pNotifier) {
        pNotifier.addListener(new RequirementMatrixReportListener());
        super.run(pNotifier);
    }

}
