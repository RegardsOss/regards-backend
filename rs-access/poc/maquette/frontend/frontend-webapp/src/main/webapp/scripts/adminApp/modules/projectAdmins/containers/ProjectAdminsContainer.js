import React, { PropTypes } from 'react'
import { connect } from 'react-redux';
// Components
import ProjectAdminsComponent from '../components/ProjectAdminsComponent'
import UserFormComponent from '../components/UserFormComponent'
// Actions
import {
  updateOrCreateProjectAdmin,
  deleteProjectAdmin } from '../actions'
import {
  selectProjectAdmin,
  showProjectAdminConfiguration,
  hideProjectAdminConfiguration } from 'adminApp/modules/ui/actions'
// Selectors
import {
  getSelectedProjectId,
  getProjectById,
  getProjectAdminsByProject,
  getSelectedProjectAdminId,
  getProjectAdminById } from 'adminApp/reducer'

class ProjectAdminsContainer extends React.Component {
  render () {
    return (
      <div>
        <ProjectAdminsComponent
          project={this.props.project}
          projectAdmins={this.props.projectAdmins}
          onAddClick={this.props.showProjectAdminConfiguration}
          onConfigureClick={this.props.showProjectAdminConfiguration}
          onDeleteClick={this.props.handleDelete} />
        <UserFormComponent
          projectAdmin={this.props.activeProjectAdmin}
          show={this.props.projectAdminConfigurationIsShown}
          onSubmit={this.props.onUserFormSubmit}
          onCancelClick={this.props.hideProjectAdminConfiguration} />
    </div>
    )
  }
}

ProjectAdminsContainer.propTypes = {
  projectAdmins: PropTypes.array
};
const mapStateToProps = (state, ownProps) => {
  let selectedProjectId = getSelectedProjectId(state)
  let selectedProject = getProjectById(state, selectedProjectId)
  let projectAdmins = getProjectAdminsByProject(state, selectedProject)
  let selectedProjectAdminId = getSelectedProjectAdminId(state)
  let selectedProjectAdmin = getProjectAdminById(state, selectedProjectAdminId)
  return {
    project: selectedProject,
    projectAdmins: projectAdmins,
    projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
    selectedProjectAdmin: selectedProjectAdmin
  }
}
const mapDispatchToProps = (dispatch) => ({
  showProjectAdminConfiguration: (id) => {
    dispatch(selectProjectAdmin(id))
    dispatch(showProjectAdminConfiguration())
  },
  hideProjectAdminConfiguration: () => dispatch(hideProjectAdminConfiguration()),
  onUserFormSubmit: (e) => {
    dispatch(updateOrCreateProjectAdmin(e.id, e.username, e.projectId))
    dispatch(hideProjectAdminConfiguration())
  },
  handleDelete: (id) => {
    dispatch(deleteProjectAdmin(id))
    dispatch(hideProjectAdminConfiguration())
  }
})
export default connect(mapStateToProps, mapDispatchToProps)(ProjectAdminsContainer);
