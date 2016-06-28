import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
// Cross-modules
// Containers
import { ProjectAdminsContainer } from 'adminApp/modules/projectAdmins'
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
import { UserFormComponent } from 'adminApp/modules/projectAdmins'
// Actions
import {
  addProject,
  deleteProject } from '../actions'
import {
  selectProject,
  showProjectConfiguration,
  hideProjectConfiguration,
  showProjectAdminConfiguration } from 'adminApp/modules/ui/actions'
// Selectors
import {
  getProjects,
  getSelectedProjectId,
  getProjectById } from 'adminApp/reducer'


class ProjectsContainer extends React.Component {
  render () {
    const {
      onSelect,
      projects,
      selectedProject,
      projectConfigurationIsShown,
      deleteProject,
      deleteProjectAdmin,
      showProjectConfiguration,
      hideProjectConfiguration,
      handleSubmit
    } = this.props

    return (
      <div>
        <ProjectConfigurationComponent
          show={projectConfigurationIsShown}
          onSubmit={handleSubmit}
          onCancelClick={hideProjectConfiguration} />
        <ManageProjectsComponent
          projects={projects}
          onSelect={onSelect}
          onAddClick={showProjectConfiguration}
          onDeleteClick={() => deleteProject(selectedProject.id)} />
        <ProjectAdminsContainer />
      </div>
    )
  }
}

ProjectsContainer.propTypes = {
  projects: PropTypes.array,
  selectedProject: PropTypes.object,
  projectConfigurationIsShown: PropTypes.bool
};
const mapStateToProps = (state) => ({
  projects: getProjects(state),
  selectedProject: getProjectById(state, getSelectedProjectId(state)),
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
})
const mapDispatchToProps = (dispatch) => ({
  onSelect:                 (e) => dispatch(selectProject(e.target.value)),
  deleteProject:            (projectId) => dispatch(deleteProject(projectId)),
  deleteProjectAdmin:       (id) => dispatch(deleteProjectAdmin(id)),
  showProjectConfiguration: () => dispatch(showProjectConfiguration()),
  hideProjectConfiguration: () => dispatch(hideProjectConfiguration()),
  handleSubmit:             (e) => {
    let idProject = "9999" // TODO
    dispatch(addProject(idProject, e.projectName))
    dispatch(hideProjectConfiguration())
    dispatch(selectProject(idProject))
  }
})
export default connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
