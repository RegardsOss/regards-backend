UPDATE t_plugin_configuration \
    SET pluginclassname = 'fr.cnes.regards.modules.storage.plugin.security.NoCatalogSecurityDelegationPlugin' \
    WHERE pluginclassname = 'fr.cnes.regards.modules.storage.domain.plugin.NoCatalogSecurityDelegationPlugin';