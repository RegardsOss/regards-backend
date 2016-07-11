"use strict";
const React = require('react');
//import icons from 'stylesheets/foundation-icons/foundation-icons.scss'
const RegardsSelect_1 = require('../../../../common/components/RegardsSelect');
class ManageProjectsComponent extends React.Component {
    render() {
        return (React.createElement("div", null, React.createElement("span", null, "Manage projects"), React.createElement("button", {title: 'Add new project', onClick: this.props.onAddClick}, React.createElement("i", {className: 'fi-plus'})), React.createElement("button", {title: 'Delete selected project', onClick: () => this.props.onDeleteClick(this.props.selectedProjectId)}, React.createElement("i", {className: 'fi-trash'})), React.createElement("br", null), React.createElement(RegardsSelect_1.default, {list: this.props.projects, label: 'Select a project', onSelect: this.props.onSelect, displayAttribute: "name", identityAttribute: "id"})));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = ManageProjectsComponent;
//# sourceMappingURL=ManageProjectsComponent.js.map