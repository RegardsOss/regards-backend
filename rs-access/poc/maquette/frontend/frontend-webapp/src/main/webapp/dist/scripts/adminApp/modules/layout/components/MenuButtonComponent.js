"use strict";
const React = require('react');
const react_router_1 = require('react-router');
// Performs a <Link> encapsulation
class MenuButtonComponent extends React.Component {
    render() {
        const { styles, label, to, icon, onClick } = this.props;
        if (to)
            return (React.createElement(react_router_1.Link, {to: to, className: styles.menuelement + " " + styles.unselected, activeClassName: styles.selected}, React.createElement("i", {className: icon, title: label}), React.createElement("span", null, label)));
        else
            return (React.createElement("span", {to: to, onClick: onClick, className: styles.menuelement + " " + styles.unselected}, React.createElement("i", {className: icon, title: label}), React.createElement("span", null, label)));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = MenuButtonComponent;
//# sourceMappingURL=MenuButtonComponent.js.map