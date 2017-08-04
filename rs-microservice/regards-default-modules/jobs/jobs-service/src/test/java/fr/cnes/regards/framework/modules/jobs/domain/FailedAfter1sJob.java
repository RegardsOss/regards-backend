package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * A job that failed after 1 s
 * @author oroussel
 */
public class FailedAfter1sJob extends AbstractJob<Void> {

    public FailedAfter1sJob() {
    }

    @Override
    public void run() {
        try {
            System.out.println("START WAITING...");
            Thread.sleep(1_000);
            System.out.println("... END WAITING");
            if (true) {
                throw new RuntimeException("DIE YOU BLOODY RASCAL !");
            }
        } catch (InterruptedException e) {
            System.out.println("INTERRUPTED in sleep");
        }
    }
}
