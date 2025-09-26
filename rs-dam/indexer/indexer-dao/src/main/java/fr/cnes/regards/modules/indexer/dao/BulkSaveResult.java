/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.indexer.dao;

import jakarta.annotation.Nullable;
import org.elasticsearch.action.DocWriteResponse.Result;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Object permitting to know what IEsRepository.saveBulk() method has really done.
 *
 * @author oroussel
 */
public class BulkSaveResult {

    private final Set<String> savedDocIds = new HashSet<>();

    /**
     * Map sessionOwner to sessions which are mapped to number of document indexed
     */
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> savedDocPerSessionOwner = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Exception> inErrorDocsMap = new ConcurrentHashMap<>();

    /**
     * Map sessionOwner to sessions which are mapped to number of document in error
     */
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> inErrorDocPerSessionOwner = new ConcurrentHashMap<>();

    /**
     * Error detailed message is set by EsRespository when notify to admin. It is then used to show error message while
     * ingesting a datasource
     */
    private String detailedErrorMsg;

    public BulkSaveResult() {
        super();
    }

    /**
     * add information needed to report document that could be indexed
     *
     * @param session      nullable, must not be null for document which are internal {@link fr.cnes.regards.modules.dam.domain.entities.DataObject}
     * @param sessionOwner nullable, must not be null for document which are internal {@link fr.cnes.regards.modules.dam.domain.entities.DataObject}
     */
    public void addSavedDoc(String docId,
                            Result docResultType,
                            @Nullable String session,
                            @Nullable String sessionOwner) {
        // Add document to the current bulk save result
        savedDocIds.add(docId);

        if (docResultType != Result.CREATED || session == null || sessionOwner == null) {
            return;
        }
        // Only notify sessions for newly created documents. Updated ones should not be notified
        // Since session and sessionOwner are provided, add them to the dispatched map by session owner too.
        putSessionAndOwner(savedDocPerSessionOwner, session, sessionOwner);
    }

    /**
     * add information needed to report document that could not be indexed
     *
     * @param session      nullable, must not be null for document which are internal {@link fr.cnes.regards.modules.dam.domain.entities.DataObject}
     * @param sessionOwner nullable, must not be null for document which are internal {@link fr.cnes.regards.modules.dam.domain.entities.DataObject}
     */
    public void addInErrorDoc(String docId,
                              Exception exception,
                              @Nullable String session,
                              @Nullable String sessionOwner) {
        // Add document to the current bulk save result
        inErrorDocsMap.put(docId, exception);

        // nothing else to be added if session or owner are missing.
        if (session == null || sessionOwner == null) {
            return;
        }

        // Since session and sessionOwner are provided, add them to the dispatched map by session owner too.
        putSessionAndOwner(inErrorDocPerSessionOwner, session, sessionOwner);
    }

    /**
     * Put the given session and owner to the given map.
     *
     * @param docPerSessionOwner Map by session owner of a Map of counter by session.
     * @param session            the session
     * @param sessionOwner       the owner
     */
    private void putSessionAndOwner(ConcurrentMap<String, ConcurrentMap<String, Long>> docPerSessionOwner,
                                    String session,
                                    String sessionOwner) {
        ConcurrentMap<String, Long> docForSessionOwner = docPerSessionOwner.computeIfAbsent(sessionOwner,
                                                                                            (owner) -> new ConcurrentHashMap<>());
        long savedDocForSession = docForSessionOwner.getOrDefault(session, 0L);
        docForSessionOwner.put(session, savedDocForSession + 1L);
    }

    public int getSavedDocsCount() {
        return savedDocIds.size();
    }

    public int getInErrorDocsCount() {
        return inErrorDocsMap.size();
    }

    public Stream<String> getSavedDocIdsStream() {
        return savedDocIds.stream();
    }

    public Stream<String> getInErrorDocIdsStream() {
        return inErrorDocsMap.keySet().stream();
    }

    public Exception getInErrorDocCause(String docId) {
        return inErrorDocsMap.get(docId);
    }

    public String getDetailedErrorMsg() {
        return detailedErrorMsg;
    }

    public void setDetailedErrorMsg(String detailedErrorMsg) {
        this.detailedErrorMsg = detailedErrorMsg;
    }

    public ConcurrentMap<String, ConcurrentMap<String, Long>> getSavedDocPerSessionOwner() {
        return savedDocPerSessionOwner;
    }

    public ConcurrentMap<String, ConcurrentMap<String, Long>> getInErrorDocPerSessionOwner() {
        return inErrorDocPerSessionOwner;
    }

    /**
     * Append another bulk save result
     *
     * @param otherBulkSaveResult another bulk save result
     * @return this
     */
    public BulkSaveResult append(BulkSaveResult otherBulkSaveResult) {
        if (otherBulkSaveResult != null) {
            this.savedDocIds.addAll(otherBulkSaveResult.savedDocIds);
            this.inErrorDocsMap.putAll(otherBulkSaveResult.inErrorDocsMap);
            this.savedDocPerSessionOwner.putAll(otherBulkSaveResult.savedDocPerSessionOwner);
            this.inErrorDocPerSessionOwner.putAll(otherBulkSaveResult.inErrorDocPerSessionOwner);
        }
        return this;
    }
}
