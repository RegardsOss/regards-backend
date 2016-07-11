"use strict";
const React = require('react');
class Time extends React.Component {
    constructor() {
        super();
    }
    render() {
        return (React.createElement("div", {class: this.props.styles.timer}, this.props.time));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = Time;
//# sourceMappingURL=TimeComponent.js.map