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
class ProjectConfigurationComponent extends react_1.Component {
    render() {
        const { show, onSaveClick, onCancelClick, fields: { projectName }, handleSubmit, submitting, styles } = this.props;
        if (show)
            return (React.createElement("form", {onSubmit: handleSubmit}, React.createElement("div", null, React.createElement("input", __assign({type: "text", placeholder: "Project Name"}, projectName))), React.createElement("div", null, React.createElement("button", {className: styles['button'] + ' ' + styles['success'], disabled: submitting}, React.createElement("i", {className: 'fi-save'}), submitting ? 'Submitting...' : '', " Save"), React.createElement("button", {type: "button", className: styles['button'] + ' ' + styles['alert'], disabled: submitting, onClick: onCancelClick}, React.createElement("i", {className: 'fi-prohibited'}), "Cancel"))));
        else
            return null;
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = redux_form_1.reduxForm({
    form: 'ProjectConfigurationForm',
    fields: ['projectName'] // all the fields in your form
})(ProjectConfigurationComponent);
//# sourceMappingURL=ProjectConfigurationComponent.js.map