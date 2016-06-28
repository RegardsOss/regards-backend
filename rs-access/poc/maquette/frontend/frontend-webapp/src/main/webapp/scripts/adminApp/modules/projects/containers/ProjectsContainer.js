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
  deleteProject } from '../actions'
import {
  selectProject,
  showProjectConfiguration,
  hideProjectConfiguration,
  showProjectAdminConfiguration } from 'adminApp/modules/ui/actions'

class ProjectsContainer extends React.Component {
  render () {
    const {
      onSelect,
      projects,
      projectConfigurationIsShown,
      deleteProject,
      deleteProjectAdmin,
      showProjectConfiguration,
      hideProjectConfiguration
    } = this.props
    const selectedProject = projects.items.find(project => project.selected)

    return (
      <div>
        <ProjectConfigurationComponent
          show={projectConfigurationIsShown}
          onSubmit={hideProjectConfiguration}
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
  projects: PropTypes.object
};
const mapStateToProps = (state) => ({
  projects: state.adminApp.projects,
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
})
const mapDispatchToProps = (dispatch) => ({
  onSelect:                 (e) => dispatch(selectProject(e.target.value)),
  deleteProject:            (projectId) => dispatch(deleteProject(projectId)),
  deleteProjectAdmin:       (id) => dispatch(deleteProjectAdmin(id)),
  showProjectConfiguration: () => dispatch(showProjectConfiguration()),
  hideProjectConfiguration: () => dispatch(hideProjectConfiguration()),
})
export default connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
