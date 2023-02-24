/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.nio.file.Path;

/**
 * Interface to provide a manager of pending actions to storage plugins during storage location pending action task.<br/>
 * <p>
 * {@link IStorageLocation} plugins can indicate that a file is successfully stored but a pending action is still remaining on the file.
 * For example a Near Line plugin can store locally the file (on a local file system) and send the file
 * in a near future. In this case, the file is stored but the send action is still pending.
 * <p><br/>
 * To handle those remaining pending actions, storage service call the method {@link IStorageLocation#runPeriodicAction(IPeriodicActionProgressManager)}
 * to allow plugins to run or check pending actions status.
 * During this task, plugins can call the method of this manager to indicates that the pending action on a given file is successfully over.
 */
public interface IPeriodicActionProgressManager {

    /**
     * Inform progress manager that a remaining pending action has been successfully for a given file url.
     */
    void storagePendingActionSucceed(String pendingActionSucceedUrl);

    /**
     * Inform progress manager that a remaining pending action has been terminated in error for a given file url.
     */
    void storagePendingActionError(Path pendingActionErrorPath);

}
