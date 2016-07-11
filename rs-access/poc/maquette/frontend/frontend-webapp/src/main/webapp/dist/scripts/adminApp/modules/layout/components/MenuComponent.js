"use strict";
const React = require('react');
const MenuButtonComponent_1 = require('./MenuButtonComponent');
// Styles
var classnames = require("classnames");
require('../../../../stylesheets/foundation-icons/foundation-icons.scss');
const ThemeUtils_1 = require('../../../../common/theme/ThemeUtils');
class Menu extends React.Component {
    render() {
        const { theme, project } = this.props;
        const styles = ThemeUtils_1.getThemeStyles(theme, 'adminApp/styles');
        const menuClassName = classnames(styles['menuContainer']);
        const ulClassName = classnames(styles['menu'], styles['vertical'], styles['icon-top']);
        return (React.createElement("div", {className: menuClassName}, React.createElement("ul", {className: ulClassName}, React.createElement("li", null, React.createElement(MenuButtonComponent_1.default, {to: "/admin/" + project + "/test", styles: styles, label: "Logout", icon: "fi-power"})), React.createElement("li", null, React.createElement(MenuButtonComponent_1.default, {to: "/admin/" + project + "/projects", styles: styles, label: "Projects", icon: "fi-widget"})))));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = Menu;
//# sourceMappingURL=MenuComponent.js.map