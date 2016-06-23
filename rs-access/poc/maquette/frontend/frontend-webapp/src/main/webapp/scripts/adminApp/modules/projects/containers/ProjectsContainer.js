import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectAdministratorsComponent from '../components/ProjectAdministratorsComponent'
import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
import UserFormComponent from '../components/UserFormComponent'
// Actions
import {
  selectProject,
  deleteProject,
  deleteProjectAdmin,
  showProjectConfiguration,
  hideProjectConfiguration,
  showAdminConfiguration,
  hideAdminConfiguration } from '../actions/ProjectsActions'

class ProjectsContainer extends React.Component {
  render () {
    const {
      projects,
      projectConfigurationIsShown,
      adminConfigurationIsShown,
      onSelect,
      deleteProject,
      deleteProjectAdmin,
      showProjectConfiguration,
      hideProjectConfiguration,
      showAdminConfiguration,
      hideAdminConfiguration
    } = this.props
    const selectedProject = projects.items.find(project => project.selected)

    return (
      <div>
        <UserFormComponent
          asyncValidating={true}
          show={adminConfigurationIsShown}
          user={"toto"}
          onSubmit={hideAdminConfiguration}
          onCancelClick={hideAdminConfiguration} />
        <ProjectConfigurationComponent
          show={projectConfigurationIsShown}
          onSubmit={hideProjectConfiguration}
          onCancelClick={hideProjectConfiguration} />
        <ManageProjectsComponent
          projects={projects}
          onSelect={onSelect}
          onAddClick={showProjectConfiguration}
          onDeleteClick={() => deleteProject(selectedProject.id)} />
        <ProjectAdministratorsComponent
          asyncValidating={true}
          project={selectedProject}
          onAddClick={showAdminConfiguration}
          onDeleteClick={deleteProjectAdmin} />
      </div>
    )
  }
}

ProjectsContainer.propTypes = {
  projects: PropTypes.object
};
const mapStateToProps = (state) => ({
  projects: state.adminApp.projects,
  projectConfigurationIsShown: state.adminApp.projects.projectConfigurationIsShown,
  adminConfigurationIsShown: state.adminApp.projects.adminConfigurationIsShown
})
const mapDispatchToProps = (dispatch) => ({
  onSelect:                 (e) => dispatch(selectProject(e.target.value)),
  deleteProject:            (projectId) => dispatch(deleteProject(projectId)),
  deleteProjectAdmin:       (id) => dispatch(deleteProjectAdmin(id)),
  showProjectConfiguration: () => dispatch(showProjectConfiguration()),
  hideProjectConfiguration: () => dispatch(hideProjectConfiguration()),
  showAdminConfiguration:   () => dispatch(showAdminConfiguration()),
  hideAdminConfiguration:   () => dispatch(hideAdminConfiguration())
})
module.exports = connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
