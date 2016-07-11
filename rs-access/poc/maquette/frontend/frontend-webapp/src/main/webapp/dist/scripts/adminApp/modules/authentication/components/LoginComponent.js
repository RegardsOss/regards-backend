"use strict";
const React = require('react');
class LoginComponent extends React.Component {
    constructor() {
        super();
        this.state = {
            username: "",
            password: "",
            error: ""
        };
        this.handleKeyPress = this.handleKeyPress.bind(this);
    }
    handleKeyPress(event) {
        if (event.key === 'Enter') {
            this.props.onLogin(this.state.username, this.state.password);
        }
    }
    render() {
        const { styles } = this.props;
        return (React.createElement("div", {className: styles["login-modal"], onKeyDown: this.handleKeyPress}, React.createElement("p", {className: styles["login-error"]}, this.props.errorMessage), React.createElement("label", {for: "username"}, "Username"), React.createElement("input", {id: "username", onChange: (event) => {
            this.setState({ "username": event.target.value });
        }}), React.createElement("br", null), React.createElement("label", {for: "password"}, "Password"), React.createElement("input", {type: "password", id: "password", onChange: (event) => {
            this.setState({ "password": event.target.value });
        }}), React.createElement("br", null), React.createElement("button", {className: styles.button, onClick: () => {
            this.props.onLogin(this.state.username, this.state.password);
        }}, "Log in")));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = LoginComponent;
//# sourceMappingURL=LoginComponent.js.map