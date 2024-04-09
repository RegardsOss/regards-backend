package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.ingest.domain.settings.IngestSettings;
import org.springframework.stereotype.Component;

@Component
public class SipBodyTimeToLiveSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public SipBodyTimeToLiveSettingCustomizer() {
        super(IngestSettings.SIP_BODY_TIME_TO_LIVE,
              "parameter [sip body time to live] can be null or must be a valid positive");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof Integer sipBodyTtl && sipBodyTtl >= -1;
    }

}
