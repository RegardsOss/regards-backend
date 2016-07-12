"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeUtils_1 = require('../../../common/theme/ThemeUtils');
const LoginComponent_1 = require('./components/LoginComponent');
const AuthenticateActions_1 = require('../../../common/authentication/AuthenticateActions');
class Authentication extends React.Component {
    constructor() {
        super();
    }
    render() {
        const styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'adminApp/styles');
        return (React.createElement(LoginComponent_1.default, {styles: styles, onLogin: this.props.onLogin, errorMessage: this.props.errorMessage}));
    }
}
const mapStateToProps = (state) => {
    return {
        errorMessage: state.common.authentication.error,
        theme: state.common.theme
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        onLogin: (userName, password) => dispatch(AuthenticateActions_1.fetchAuthenticate(userName, password))
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(Authentication);
//# sourceMappingURL=Authentication.js.map