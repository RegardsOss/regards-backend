"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var React = require('react');
var LoginComponent = (function (_super) {
    __extends(LoginComponent, _super);
    function LoginComponent() {
        _super.call(this);
        this.state = {
            username: "",
            password: "",
            error: ""
        };
        this.handleKeyPress = this.handleKeyPress.bind(this);
    }
    LoginComponent.prototype.handleKeyPress = function (event) {
        if (event.key === 'Enter') {
            this.props.onLogin(this.state.username, this.state.password);
        }
    };
    LoginComponent.prototype.render = function () {
        var _this = this;
        var styles = this.props.styles;
        return (React.createElement("div", {className: styles["login-modal"], onKeyDown: this.handleKeyPress}, 
            React.createElement("p", {className: styles["login-error"]}, this.props.errorMessage), 
            React.createElement("label", {for: "username"}, "Username"), 
            React.createElement("input", {id: "username", onChange: function (event) {
                _this.setState({ "username": event.target.value });
            }}), 
            React.createElement("br", null), 
            React.createElement("label", {for: "password"}, "Password"), 
            React.createElement("input", {type: "password", id: "password", onChange: function (event) {
                _this.setState({ "password": event.target.value });
            }}), 
            React.createElement("br", null), 
            React.createElement("button", {className: styles.button, onClick: function () {
                _this.props.onLogin(_this.state.username, _this.state.password);
            }}, "Log in")));
    };
    return LoginComponent;
}(React.Component));
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = LoginComponent;
//# sourceMappingURL=LoginComponent.js.map