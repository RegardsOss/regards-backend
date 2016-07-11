"use strict";
const React = require('react');
const RegardsSelect = ({ list = [], onSelect, identityAttribute = 'id', displayAttribute = 'name', label }) => {
    return (React.createElement("select", {onChange: onSelect}, 
        React.createElement("option", {defaultValue: true}, label), 
        list.map(item => React.createElement("option", {key: item[identityAttribute], value: item[identityAttribute]}, item[displayAttribute]))));
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = RegardsSelect;
//# sourceMappingURL=RegardsSelect.js.map