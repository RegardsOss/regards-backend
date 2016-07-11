"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
// Containers
const UserFormContainer_1 = require('./UserFormContainer');
// Components
const ProjectAdminsComponent_1 = require('../components/ProjectAdminsComponent');
// Actions
const actions_1 = require('../actions');
const actions_2 = require('../../ui/actions');
// Selectors
const reducer_1 = require('../../../reducer');
// Styles
var classnames = require('classnames');
require('../../../../../stylesheets/foundation-icons/foundation-icons.scss');
const ThemeUtils_1 = require('../../../../common/theme/ThemeUtils');
class ProjectAdminsContainer extends React.Component {
    componentWillReceiveProps(nextProps) {
        const oldProject = this.props.project;
        const nextProject = nextProps.project;
        if (nextProject && nextProject != oldProject) {
            const link = nextProject.links.find((link) => link.rel === "users");
            if (link) {
                const href = link.href;
                this.props.fetchProjectAdminsBy(href);
            }
        }
    }
    render() {
        return (React.createElement("div", null, React.createElement(ProjectAdminsComponent_1.default, {project: this.props.project, projectAdmins: this.props.projectAdmins, onAddClick: this.props.showProjectAdminConfiguration, onConfigureClick: this.props.showProjectAdminConfiguration, onDeleteClick: this.props.handleDelete, styles: this.props.styles}), React.createElement(UserFormContainer_1.default, {show: this.props.projectAdminConfigurationIsShown, handleSubmit: this.props.onUserFormSubmit, onCancelClick: this.props.hideProjectAdminConfiguration, styles: this.props.styles})));
    }
}
const mapStateToProps = (state, ownProps) => {
    const selectedProjectId = reducer_1.getSelectedProjectId(state);
    const selectedProject = reducer_1.getProjectById(state, selectedProjectId);
    const projectAdmins = reducer_1.getProjectAdmins(state); // TODO: By project: getProjectAdminsByProject(state, selectedProject)
    const selectedProjectAdminId = reducer_1.getSelectedProjectAdminId(state);
    const selectedProjectAdmin = reducer_1.getProjectAdminById(state, selectedProjectAdminId);
    return {
        project: selectedProject,
        projectAdmins: projectAdmins,
        projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
        selectedProjectAdmin: selectedProjectAdmin,
        styles: ThemeUtils_1.getThemeStyles(state.common.theme, 'adminApp/styles')
    };
};
const mapDispatchToProps = (dispatch) => ({
    fetchProjectAdminsBy: (href) => dispatch(actions_1.fetchProjectAdminsBy(href)),
    showProjectAdminConfiguration: (id) => {
        dispatch(actions_2.selectProjectAdmin(id));
        dispatch(actions_2.showProjectAdminConfiguration());
    },
    hideProjectAdminConfiguration: () => dispatch(actions_2.hideProjectAdminConfiguration()),
    onUserFormSubmit: (e) => {
        dispatch(actions_1.updateOrCreateProjectAdmin(e.id, e.username, e.projectId));
        dispatch(actions_2.hideProjectAdminConfiguration());
    },
    handleDelete: (id) => {
        dispatch(actions_1.deleteProjectAdmin(id));
        dispatch(actions_2.hideProjectAdminConfiguration());
    }
});
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(ProjectAdminsContainer);
//# sourceMappingURL=ProjectAdminsContainer.js.map