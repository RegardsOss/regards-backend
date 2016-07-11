"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
class PluginComponent extends React.Component {
    render() {
        const { plugin } = this.props;
        if (plugin && plugin.plugin) {
            return React.createElement(plugin.plugin, null);
        }
        else {
            return React.createElement("div", {className: "error"}, 
                " Undefined plugin ", 
                plugin.name, 
                " ");
        }
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect()(PluginComponent);
//# sourceMappingURL=PluginComponent.js.map