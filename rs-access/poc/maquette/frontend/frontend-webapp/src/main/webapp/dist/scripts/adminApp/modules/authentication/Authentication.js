"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var React = require('react');
var react_redux_1 = require('react-redux');
var ThemeUtils_1 = require('../../../common/theme/ThemeUtils');
var LoginComponent_1 = require('./components/LoginComponent');
var AuthenticateActions_1 = require('../../../common/authentication/AuthenticateActions');
var Authentication = (function (_super) {
    __extends(Authentication, _super);
    function Authentication() {
        _super.call(this);
    }
    Authentication.prototype.render = function () {
        var styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'adminApp/styles');
        return (React.createElement(LoginComponent_1.default, {styles: styles, onLogin: this.props.onLogin, errorMessage: this.props.errorMessage}));
    };
    return Authentication;
}(React.Component));
var mapStateToProps = function (state) {
    return {
        errorMessage: state.authentication.error,
        theme: state.theme
    };
};
var mapDispatchToProps = function (dispatch) {
    return {
        onLogin: function (userName, password) { return dispatch(AuthenticateActions_1.fetchAuthenticate(userName, password)); }
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(Authentication);
//# sourceMappingURL=Authentication.js.map