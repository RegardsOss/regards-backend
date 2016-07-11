"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const AccessRightsComponent_1 = require('../../../common/access-rights/AccessRightsComponent');
const PluginComponent_1 = require('common/plugins/PluginComponent');
class PluginContainer extends AccessRightsComponent_1.default {
    getDependencies() {
        const { plugin } = this.props;
        if (plugin && plugin.getDependencies) {
            return plugin.getDependencies();
        }
        else {
            return null;
        }
    }
    render() {
        if (this.state.access === true) {
            console.log("Rendering module");
            // this.props : parameters passed by react component
            // this.props.params : parameters passed by react router
            const { params, plugins } = this.props;
            if (plugins) {
                const plugin = plugins.find(plugin => {
                    return plugin.name === params.plugin;
                });
                // Get plugin from store
                return React.createElement(PluginComponent_1.default, {plugin: plugin});
            }
        }
        return null;
    }
}
const mapStateToProps = (state) => {
    return {
        plugins: state.plugins.items
    };
};
const pluginConnected = react_redux_1.connect(mapStateToProps)(PluginContainer);
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = pluginConnected;
module.exports = pluginConnected;
//# sourceMappingURL=Plugin.js.map