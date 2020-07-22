package fr.cnes.regards.modules.processing.plugins;

import io.vavr.collection.Seq;

public interface IProcessRightsDefinition {

    Seq<String> allowedTenants();

    Seq<String> allowedUserRoles();

    Seq<String> allowedDatasets();

}
