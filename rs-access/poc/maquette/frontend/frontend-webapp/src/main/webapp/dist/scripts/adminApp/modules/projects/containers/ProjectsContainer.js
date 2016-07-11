"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeUtils_1 = require('../../../../common/theme/ThemeUtils');
var classnames = require('classnames');
const lodash_1 = require('lodash');
const projectAdmins_1 = require('../../projectAdmins');
const ManageProjectsComponent_1 = require('../components/ManageProjectsComponent');
const ProjectConfigurationComponent_1 = require('../components/ProjectConfigurationComponent');
const actions_1 = require('../actions');
const actions_2 = require('../../ui/actions');
const reducer_1 = require('../../../reducer');
class ProjectsContainer extends React.Component {
    componentWillMount() {
        this.props.onLoad();
    }
    render() {
        const className = classnames(this.props.styles['columns'], this.props.styles['small-4'], this.props.styles['callout'], this.props.styles['custom-callout']);
        return (React.createElement("fieldset", {className: className}, 
            React.createElement("legend", null, "Projects"), 
            React.createElement(ProjectConfigurationComponent_1.default, {styles: this.props.styles, show: this.props.projectConfigurationIsShown, handleSubmit: this.props.handleSubmit, onCancelClick: this.props.hideProjectConfiguration, submitting: true}), 
            React.createElement(ManageProjectsComponent_1.default, {styles: this.props.styles, projects: this.props.projects, selectedProjectId: this.props.selectedProjectId, onSelect: this.props.onSelect, onAddClick: this.props.showProjectConfiguration, onDeleteClick: this.props.deleteProject}), 
            React.createElement(projectAdmins_1.ProjectAdminsContainer, null)));
    }
}
const mapStateToProps = (state) => ({
    projects: lodash_1.map(reducer_1.getProjects(state).items, (value, key) => ({ id: key, name: value.name })),
    selectedProjectId: reducer_1.getSelectedProjectId(state),
    projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
    styles: ThemeUtils_1.getThemeStyles(state.common.theme, 'adminApp/styles')
});
const mapDispatchToProps = (dispatch) => ({
    onLoad: () => dispatch(actions_1.fetchProjects()),
    onSelect: (e) => dispatch(actions_2.selectProject(e.target.value)),
    deleteProject: (id) => dispatch(actions_1.deleteProject(id)),
    showProjectConfiguration: () => dispatch(actions_2.showProjectConfiguration()),
    hideProjectConfiguration: () => dispatch(actions_2.hideProjectConfiguration()),
    handleSubmit: (e) => {
        let idProject = "9999";
        dispatch(actions_1.addProject(idProject, e.projectName));
        dispatch(actions_2.hideProjectConfiguration());
        dispatch(actions_2.selectProject(idProject));
    }
});
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
//# sourceMappingURL=ProjectsContainer.js.map