"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const AccessRightsComponent_1 = require('../../../../common/access-rights/AccessRightsComponent');
const LinkComponent_1 = require('../components/LinkComponent');
class NavigationContainer extends AccessRightsComponent_1.default {
    getDependencies() {
        return null;
    }
    render() {
        const { location, plugins, project } = this.props;
        if (this.state.access === true && plugins.items) {
            return (React.createElement("nav", null, 
                React.createElement(LinkComponent_1.default, {location: location, key: "plop", to: "/user/" + project + "/test"}, "Test de lien"), 
                React.createElement(LinkComponent_1.default, {location: location, key: "time", to: "/user/" + project + "/time"}, "Temps"), 
                plugins.items.map(plugin => {
                    if (plugin && plugin.plugin) {
                        return (React.createElement(LinkComponent_1.default, {location: location, key: plugin.name, to: "/user/" + project + "/plugins/" + plugin.name}, plugin.name));
                    }
                })));
        }
        return null;
    }
}
const mapStateToProps = (state) => {
    return {
        plugins: state.plugins
    };
};
const navigation = react_redux_1.connect(mapStateToProps)(NavigationContainer);
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = navigation;
//# sourceMappingURL=NavigationContainer.js.map