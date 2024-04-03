package fr.cnes.regards.framework.modules.dump.service.settings;

import fr.cnes.regards.framework.modules.dump.domain.DumpParameters;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.scheduler.AbstractDumpScheduler;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DumpParametersSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    private final AbstractDumpScheduler dumpScheduler;

    public DumpParametersSettingCustomizer(@Lazy AbstractDumpScheduler dumpScheduler) {
        super(DumpSettings.DUMP_PARAMETERS, "parameter [dump parameters] must be a valid positive number");
        this.dumpScheduler = dumpScheduler;
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        dumpScheduler.update();
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value instanceof DumpParameters dumpParameters
               && CronExpression.isValidExpression(dumpParameters.getCronTrigger())
               && isValidPath(dumpParameters.getDumpLocation());
    }

    @SuppressWarnings("java:S1166") // Rethrow exception
    private boolean isValidPath(String location) {
        boolean isValidPath = false;
        try {
            Path path = Paths.get(location);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            isValidPath = true;
        } catch (InvalidPathException | IOException e) {
            // do nothing
        }
        return isValidPath;
    }

}
