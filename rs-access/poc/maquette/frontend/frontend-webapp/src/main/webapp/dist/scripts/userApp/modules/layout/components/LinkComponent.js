"use strict";
const React = require('react');
const react_router_1 = require('react-router');
class Linkcomponent extends React.Component {
    render() {
        // to props is passed throught the react component creation
        // children props is the children of te curent component. This props is autmatically set
        // by react when creatin the component.
        const { to, children } = this.props;
        const style = { "fontSize": "20px", "lineHeight": "50px", margin: "0px 20px", "textDecoration": "none" };
        const activeStyle = { 'borderBottom': '2px solid Red' };
        return (React.createElement(react_router_1.Link, {to: to, activeStyle: activeStyle, style: style}, children));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = Linkcomponent;
//# sourceMappingURL=LinkComponent.js.map