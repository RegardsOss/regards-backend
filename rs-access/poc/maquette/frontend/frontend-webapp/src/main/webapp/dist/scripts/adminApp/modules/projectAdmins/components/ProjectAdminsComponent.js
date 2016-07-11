"use strict";
const React = require('react');
require('../../../../../stylesheets/foundation-icons/foundation-icons.scss');
const lodash_1 = require('lodash');
var classnames = require('classnames');
class ProjectAdminsComponent extends React.Component {
    render() {
        const { project, projectAdmins, styles, onAddClick, onConfigureClick, onDeleteClick } = this.props;
        if (project) {
            const className = classnames(styles['callout'], styles['custom-callout']);
            return (React.createElement("div", {className: className}, 
                "Project Administrators", 
                React.createElement("button", {title: 'Add new administrator', onClick: () => onAddClick(project.id)}, 
                    React.createElement("i", {className: 'fi-plus'})
                ), 
                React.createElement("br", null), 
                "List of administrators for ", 
                project.name, 
                ":", 
                React.createElement("ul", null, lodash_1.map(projectAdmins.items, (projectAdmin, id) => (React.createElement("li", {key: id}, 
                    projectAdmin.name, 
                    React.createElement("button", {title: 'Configure admin user', onClick: () => onConfigureClick(id)}, 
                        React.createElement("i", {className: 'fi-wrench'})
                    ), 
                    React.createElement("button", {title: 'Delete admin user', onClick: () => onDeleteClick(id)}, 
                        React.createElement("i", {className: 'fi-trash'})
                    )))))));
        }
        else {
            return null;
        }
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = ProjectAdminsComponent;
//# sourceMappingURL=ProjectAdminsComponent.js.map