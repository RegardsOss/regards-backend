package fr.cnes.regards.framework.modules.dump.service.settings;

import fr.cnes.regards.framework.modules.dump.domain.DumpParameters;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.scheduler.AbstractDumpScheduler;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Component
public class DumpParametersSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final AbstractDumpScheduler dumpScheduler;

    public DumpParametersSettingCustomizer(@Lazy AbstractDumpScheduler dumpScheduler) {
        this.dumpScheduler = dumpScheduler;
    }

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [dump parameters] must be a valid positive number.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [dump parameters] must be a valid positive number.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DumpSettings.DUMP_PARAMETERS.equals(dynamicTenantSetting.getName());
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        dumpScheduler.update();
    }

    private boolean isProperValue(Object value) {
        return value instanceof DumpParameters dumpParameters
               && CronExpression.isValidExpression(dumpParameters.getCronTrigger())
               && isValidPath(dumpParameters.getDumpLocation());
    }

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
