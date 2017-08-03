package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * 10 s job
 * @author oroussel
 */
public class LongJob extends AbstractJob implements IJob {

    @Override
    public void run() {
        try {
            for (int i = 0; i < this.getCompletionCount(); i++) {
                Thread.sleep(1_000);
                super.advanceCompletion();
            }
        } catch (InterruptedException e) {
            // Don't give a shit
        }
    }

    @Override
    public int getCompletionCount() {
        return 10;
    }
}
