package fr.cnes.regards.modules.indexer.dao.mapping.model;

import io.vavr.collection.Map;

public class TimingAnalysis {
    final long indexingMs;
    final Map<String, Long> queryMeanMs;
    public TimingAnalysis(long indexingMs, Map<String, Long> queryMeanMs) {
        this.indexingMs = indexingMs;
        this.queryMeanMs = queryMeanMs;
    }
    public Map<String, Long> getQueryMeanMs() {
        return queryMeanMs;
    }
    public long getIndexingMs() {
        return indexingMs;
    }
}
