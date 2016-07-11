"use strict";
const React = require('react');
const react_router_1 = require('react-router');
class ProjectComponent extends React.Component {
    render() {
        // styles props is passed throught the react component creation
        const { styles } = this.props;
        return (React.createElement("li", {className: styles.link}, React.createElement("p", null, this.props.project.name), React.createElement(react_router_1.Link, {to: "/user/" + this.props.project.name, className: styles["project-link"]}, "ihm user"), React.createElement(react_router_1.Link, {to: "/admin/" + this.props.project.name, className: styles["project-link"]}, "ihm admin")));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = ProjectComponent;
//# sourceMappingURL=ProjectComponent.js.map