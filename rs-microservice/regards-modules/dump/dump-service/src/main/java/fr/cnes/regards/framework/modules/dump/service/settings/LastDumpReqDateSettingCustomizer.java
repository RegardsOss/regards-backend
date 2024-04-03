package fr.cnes.regards.framework.modules.dump.service.settings;

import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class LastDumpReqDateSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public LastDumpReqDateSettingCustomizer() {
        super(DumpSettings.LAST_DUMP_REQ_DATE,
              "parameter [last dump request date] can be null or must be a valid offset date time (ISO-8601 format)");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof OffsetDateTime;
    }

}
