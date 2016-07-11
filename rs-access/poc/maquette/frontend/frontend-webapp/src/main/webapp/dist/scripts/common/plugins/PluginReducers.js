"use strict";
const PluginsActions_1 = require('./PluginsActions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        isFetching: false,
        items: [],
        lastUpdate: ''
    }, action) => {
    switch (action.type) {
        case PluginsActions_1.REQUEST_PLUGINS:
            return Object.assign({}, state, {
                isFetching: true
            });
        case PluginsActions_1.RECEIVE_PLUGINS:
            return Object.assign({}, state, {
                isFetching: false,
                items: action.plugins,
                lastUpdate: action.receivedAt
            });
        case PluginsActions_1.FAILED_PLUGINS:
            return Object.assign({}, state, {
                isFetching: false
            });
        case PluginsActions_1.PLUGIN_INITIALIZED:
            let result = Object.assign({}, state);
            result.items = result.items.map(plugin => {
                return Object.assign({}, plugin, {
                    plugin: action.plugin
                });
            });
            return result;
        default:
            return state;
    }
};
// const pluginsReducers = {
//   plugins
// }
//
// export default pluginsReducers
//# sourceMappingURL=PluginReducers.js.map