import React, { PropTypes } from 'react'
import { connect } from 'react-redux';
// Components
import ProjectAdminsComponent from '../components/ProjectAdminsComponent'
import UserFormComponent from '../components/UserFormComponent'
// actions
import {
  updateOrCreateProjectAdmin,
  deleteProjectAdmin } from '../actions'
import {
  selectProjectAdmin,
  showProjectAdminConfiguration,
  hideProjectAdminConfiguration } from 'adminApp/modules/ui/actions'

class ProjectAdminsContainer extends React.Component {
  render () {
    return (
      <div>
        <ProjectAdminsComponent
          project={this.props.project}
          projectAdmins={this.props.projectAdmins}
          onAddClick={this.props.showProjectAdminConfiguration}
          onConfigureClick={this.props.showProjectAdminConfiguration}
          onDeleteClick={this.props.hideProjectAdminConfiguration} />
        <UserFormComponent
          projectAdmin={this.props.activeProjectAdmin}
          show={this.props.projectAdminConfigurationIsShown}
          onSubmit={this.props.onUserFormSubmit}
          onCancelClick={this.props.hideProjectAdminConfiguration} />
    </div>
    )
  }
}

const getProjectAdminsForProject = (projectAdmins, project) => {
  if(!project) return []
  return projectAdmins.filter(pa => pa.projects.includes(project.id))
}

const getSelectedProject = (projects) => {
  return projects.find(p => p.selected)
}

const getProjectById = (projects, id) => {
  return projects.find(p => p.id === id)
}

ProjectAdminsContainer.propTypes = {
  projectAdmins: PropTypes.array
};
const mapStateToProps = (state, ownProps) => ({
  project: getProjectById(state.adminApp.projects.items, state.adminApp.ui.selectedProject),
  projectAdmins: getProjectAdminsForProject(state.adminApp.projectAdmins, getProjectById(state.adminApp.projects.items, state.adminApp.ui.selectedProject)),
  projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
  selectedProjectAdmin: state.adminApp.ui.selectedProjectAdmin
})
const mapDispatchToProps = (dispatch) => ({
  showProjectAdminConfiguration: (id) => {
    dispatch(selectProjectAdmin(id))
    dispatch(showProjectAdminConfiguration())
  },
  hideProjectAdminConfiguration: () => dispatch(hideProjectAdminConfiguration()),
  deleteProjectAdmin: (id) => dispatch(deleteProjectAdmin(id)),
  onUserFormSubmit: (e) => {
    dispatch(updateOrCreateProjectAdmin(e.id, e.username, ["1", "2"]))
    dispatch(hideProjectAdminConfiguration())
  }
})
export default connect(mapStateToProps, mapDispatchToProps)(ProjectAdminsContainer);
