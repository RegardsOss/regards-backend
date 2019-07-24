/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * @author sbinda
 *
 */
@Component
public class AddFileReferenceFlowItemHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<AddFileRefFlowItem> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private FileReferenceService fileRefService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(AddFileRefFlowItem.class, this);
    }

    @Override
    public void handle(TenantWrapper<AddFileRefFlowItem> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        try {
            AddFileRefFlowItem item = wrapper.getContent();
            FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(item.getChecksum(), item.getAlgorithm(),
                    item.getFileName(), item.getFileSize(), MediaType.valueOf(item.getMimeType()));
            fileRefService.addFileReference(Lists.newArrayList(item.getOwner()), metaInfo, item.getOrigine(),
                                            item.getDestination());
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

}
