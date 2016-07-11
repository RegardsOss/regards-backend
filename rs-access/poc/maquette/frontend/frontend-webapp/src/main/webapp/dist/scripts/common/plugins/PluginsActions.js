"use strict";
const fetch = require('isomorphic-fetch');
var scriptjs = require('scriptjs');
const PLUGINS_API = 'http://localhost:8080/api/plugins';
exports.REQUEST_PLUGINS = 'REQUEST_PLUGINS';
function requestPlugins() {
    return {
        type: exports.REQUEST_PLUGINS,
    };
}
exports.RECEIVE_PLUGINS = 'RECEIVE_PLUGINS';
function receivePlugins(plugins) {
    return {
        type: exports.RECEIVE_PLUGINS,
        plugins: plugins,
        receivedAt: Date.now()
    };
}
exports.FAILED_PLUGINS = 'FAILED_PLUGINS';
function failedPlugins(error) {
    return {
        type: exports.FAILED_PLUGINS,
        error: error
    };
}
exports.PLUGIN_INITIALIZED = 'PLUGIN_INITIALIZED';
function pluginInitialized(name, plugin) {
    return {
        type: exports.PLUGIN_INITIALIZED,
        name: name,
        plugin: plugin,
        error: ''
    };
}
function checkResponseStatus(response) {
    if (response.status === 200) {
        return response;
    }
    else {
        throw new Error(response.statusText);
    }
}
function fetchPlugins() {
    return function (dispatch, getState) {
        document.addEventListener('plugin', function (plugin) {
            dispatch(pluginInitialized(plugin.name, plugin.plugin));
        }, false);
        dispatch(requestPlugins());
        let authorization = "Basic";
        if (getState().authentication && getState().authentication.user && getState().authentication.user.access_token) {
            authorization = "Bearer " + getState().authentication.user.access_token;
        }
        return fetch(PLUGINS_API, {
            headers: {
                'Accept': 'application/json',
                'Authorization': authorization
            }
        })
            .then(checkResponseStatus)
            .then(function (response) {
            return response.json();
        }).then(function (body) {
            dispatch(receivePlugins(body));
            body.map((plugin) => {
                const paths = plugin.paths.map(path => {
                    return window.location.origin + "/plugins/" + path;
                });
                scriptjs(paths, plugin.name);
            });
        }).catch(function (error) {
            dispatch(failedPlugins(error));
        });
    };
}
exports.fetchPlugins = fetchPlugins;
//# sourceMappingURL=PluginsActions.js.map