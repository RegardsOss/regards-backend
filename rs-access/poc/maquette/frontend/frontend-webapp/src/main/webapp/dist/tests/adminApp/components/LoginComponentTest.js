"use strict";
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
const react_1 = require('react');
const enzyme_1 = require('enzyme');
const chai_1 = require('chai');
const LoginComponent_1 = require('../../../scripts/adminApp/modules/authentication/components/LoginComponent');
// Test a component rendering
describe('Testing login component', () => {
    it('Should render correctly the login component', () => {
        const onLogin = (username, password) => { };
        const loginStyles = {
            "login-modal": 'login-modal',
            "login-error": "login-error"
        };
        let props = {
            onLogin: onLogin,
            errorMessage: '',
            styles: loginStyles
        };
        const wrapper = enzyme_1.shallow(react_1.default.createElement(LoginComponent_1.default, __assign({}, props)));
        chai_1.expect(wrapper.find("div .login-modal")).to.have.length(1);
        chai_1.expect(wrapper.find("div .login-error")).to.have.length(1);
        chai_1.expect(wrapper.find("input #username")).to.have.length(1);
        chai_1.expect(wrapper.find("input #password")).to.have.length(1);
        chai_1.expect(wrapper.find("button")).to.have.length(1);
        // Test onLogin action
        wrapper.find("button").simulate('click');
    });
    it('Should active login action correctly', () => {
        const onLogin = (username, password) => {
            chai_1.expect(username).to.equal("test");
            chai_1.expect(password).to.equal("test_password");
        };
        const handleKeyPress = () => { };
        const loginStyles = {
            "login-modal": 'login-modal',
            "login-error": "login-error"
        };
        let props = {
            onLogin: onLogin,
            errorMessage: '',
            styles: loginStyles
        };
        const wrapper = enzyme_1.shallow(react_1.default.createElement(LoginComponent_1.default, __assign({}, props)));
        const username = wrapper.find("input #username");
        username.simulate('change', { target: { value: 'test' } });
        const password = wrapper.find("input #password");
        password.simulate('change', { target: { value: 'test_password' } });
        // Test onLogin action
        wrapper.find("button").simulate('click');
    });
});
//# sourceMappingURL=LoginComponentTest.js.map