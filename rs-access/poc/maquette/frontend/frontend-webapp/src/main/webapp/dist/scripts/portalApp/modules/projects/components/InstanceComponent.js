"use strict";
const React = require('react');
const react_router_1 = require('react-router');
class InstanceComponent extends React.Component {
    render() {
        // styles props is passed throught the react component creation
        const { styles } = this.props;
        return (React.createElement("div", {className: styles["instance-link"]}, "Accès direct à l'ihm d'administration de l'instance :", React.createElement(react_router_1.Link, {to: "/admin/instance"}, "ihm admin instance"), React.createElement("br", null)));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = InstanceComponent;
//# sourceMappingURL=InstanceComponent.js.map