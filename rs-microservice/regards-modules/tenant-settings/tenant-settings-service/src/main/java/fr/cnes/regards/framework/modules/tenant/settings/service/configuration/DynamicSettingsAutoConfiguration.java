/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.tenant.settings.service.configuration;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.sensitive.ISensitiveAnnotationEncryptionService;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingRepositoryService;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingWithMaskService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.DynamicSettingsEncryptionService;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.SensitiveDynamicSettingConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Service configuration for dynamic tenant settings.
 *
 * @author Iliana Ghazali
 **/
@Configuration
public class DynamicSettingsAutoConfiguration {

    @Bean("dynamicTenantSettingService")
    @Primary
    public DynamicTenantSettingService dynamicTenantSettingService(List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList,
                                                                   DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService) {
        return new DynamicTenantSettingService(dynamicTenantSettingCustomizerList,
                                               dynamicTenantSettingRepositoryService);
    }

    @Bean("dynamicTenantSettingServiceWithMask")
    public DynamicTenantSettingWithMaskService dynamicTenantSettingWithMaskService(DynamicTenantSettingService dynamicTenantSettingService,
                                                                                   DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService,
                                                                                   SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter) {
        return new DynamicTenantSettingWithMaskService(dynamicTenantSettingService,
                                                       dynamicTenantSettingRepositoryService,
                                                       sensitiveDynamicSettingConverter);
    }

    @Bean
    SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter(DynamicSettingsEncryptionService dynamicSettingsEncryptionService) {
        return new SensitiveDynamicSettingConverter(dynamicSettingsEncryptionService);
    }

    @Bean
    public DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService(IDynamicTenantSettingRepository dynamicTenantSettingRepository,
                                                                                       SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter) {
        return new DynamicTenantSettingRepositoryService(dynamicTenantSettingRepository,
                                                         sensitiveDynamicSettingConverter);
    }

    @Bean
    public DynamicSettingsEncryptionService dynamicSettingsEncryptionService(IEncryptionService encryptionService,
                                                                             ISensitiveAnnotationEncryptionService sensitiveEncryptionService) {
        return new DynamicSettingsEncryptionService(encryptionService, sensitiveEncryptionService);
    }

}
