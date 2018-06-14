package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.gml.impl;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.google.gson.Gson;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.rome.feed.CopyFrom;

import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.gml.GmlTimeModule;

public class GmlTimeModuleImpl extends GeoRSSModule implements GmlTimeModule, Serializable {

    private static final long serialVersionUID = 1L;

    private OffsetDateTime startDate;

    private OffsetDateTime stopDate;

    private Gson gson;

    public GmlTimeModuleImpl() {
        super(GmlTimeModuleImpl.class, GmlTimeModuleImpl.URI);
    }

    @Override
    public Class<? extends CopyFrom> getInterface() {
        return GmlTimeModule.class;
    }

    @Override
    public void copyFrom(CopyFrom obj) {
        GmlTimeModuleImpl mod = (GmlTimeModuleImpl) obj;
        mod.setStartDate(this.getStartDate());
        mod.setStopDate(this.getStopDate());
        mod.setGsonBuilder(this.getGsonBuilder());
    }

    @Override
    public OffsetDateTime getStartDate() {
        return this.startDate;
    }

    @Override
    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;

    }

    @Override
    public OffsetDateTime getStopDate() {
        return this.stopDate;
    }

    @Override
    public void setStopDate(OffsetDateTime stopDate) {
        this.stopDate = stopDate;
    }

    @Override
    public Gson getGsonBuilder() {
        return this.gson;
    }

    @Override
    public void setGsonBuilder(Gson gson) {
        this.gson = gson;
    }

}
