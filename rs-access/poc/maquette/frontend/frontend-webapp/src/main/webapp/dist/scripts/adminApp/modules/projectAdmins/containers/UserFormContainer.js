"use strict";
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
const React = require('react');
const react_1 = require('react');
const redux_form_1 = require('redux-form');
exports.fields = ['id', 'projectId', 'username', 'password', 'passwordConfirm'];
require('../../../../../stylesheets/foundation-icons/foundation-icons.scss');
const reducer_1 = require('../../../reducer');
const validate = (values) => {
    const errors = {};
    if (!values.username) {
        errors.username = 'Required';
    }
    if (!values.password) {
        errors.password = 'Required';
    }
    if (!values.passwordConfirm) {
        errors.passwordConfirm = 'Required';
    }
    if (values.password && values.passwordConfirm && values.password !== values.passwordConfirm) {
        errors.passwordConfirm = 'Must match the password';
    }
    return errors;
};
const asyncValidate = (values) => {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (['john', 'paul', 'george', 'ringo'].find((value) => value === values.username)) {
                reject({ username: 'That username is taken' });
            }
            else {
                resolve();
            }
        }, 500);
    });
};
class AsynchronousBlurValidationForm extends react_1.Component {
    render() {
        const { asyncValidating, fields: { id, projectId, username, password, passwordConfirm }, resetForm, handleSubmit, submitting, show, onCancelClick, styles } = this.props;
        if (show)
            return (React.createElement("form", {onSubmit: handleSubmit}, 
                React.createElement("div", null, 
                    React.createElement("div", null, 
                        React.createElement("input", __assign({type: "hidden", placeholder: "Id"}, id))
                    ), 
                    React.createElement("div", null, 
                        React.createElement("input", __assign({type: "hidden", placeholder: "Project Id"}, projectId))
                    ), 
                    React.createElement("label", null, "Username"), 
                    React.createElement("div", null, 
                        React.createElement("input", __assign({type: "text", placeholder: "Username"}, username)), 
                        asyncValidating === 'username' && React.createElement("i", null)), 
                    username.touched && username.error && React.createElement("div", null, username.error)), 
                React.createElement("div", null, 
                    React.createElement("label", null, "Password"), 
                    React.createElement("div", null, 
                        React.createElement("input", __assign({type: "password", placeholder: "Password"}, password))
                    ), 
                    password.touched && password.error && React.createElement("div", null, password.error)), 
                React.createElement("div", null, 
                    React.createElement("label", null, "Password (confirm)"), 
                    React.createElement("div", null, 
                        React.createElement("input", __assign({type: "password", placeholder: "Confirm your password"}, passwordConfirm))
                    ), 
                    passwordConfirm.touched && passwordConfirm.error && React.createElement("div", null, passwordConfirm.error)), 
                React.createElement("div", null, 
                    React.createElement("button", {type: "submit", className: styles['button'] + ' ' + styles['success'], disabled: submitting}, 
                        React.createElement("i", {className: 'fi-save'}), 
                        submitting ? 'Submitting...' : '', 
                        " Save"), 
                    React.createElement("button", {type: "button", className: styles['button'] + ' ' + styles['alert'], disabled: submitting, onClick: onCancelClick}, 
                        React.createElement("i", {className: 'fi-prohibited'}), 
                        "Cancel"))));
        else
            return null;
    }
}
const mapStateToProps = (state) => {
    const selectedProjectAdminId = reducer_1.getSelectedProjectAdminId(state);
    const selectedProjectAdmin = reducer_1.getProjectAdminById(state, selectedProjectAdminId);
    const selectedProjectId = reducer_1.getSelectedProjectId(state);
    return {
        initialValues: {
            id: selectedProjectAdminId,
            username: selectedProjectAdmin ? selectedProjectAdmin.name : '',
            projectId: selectedProjectId
        }
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = redux_form_1.reduxForm({
    form: 'asynchronousBlurValidation',
    fields,
    asyncValidate,
    asyncBlurFields: ['username'],
    validate
}, mapStateToProps)(AsynchronousBlurValidationForm);
//# sourceMappingURL=UserFormContainer.js.map