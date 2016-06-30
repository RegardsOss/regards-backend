import React, { PropTypes } from 'react'
import { connect } from 'react-redux';
import { getThemeStyles } from 'common/theme/ThemeUtils'
// Components
import ProjectAdminsComponent from '../components/ProjectAdminsComponent'
import UserFormComponent from '../components/UserFormComponent'
// Actions
import {
  fetchProjectAdmins,
  fetchProjectAdminsBy,
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
  getProjectAdmins,
  getProjectAdminsByProject,
  getSelectedProjectAdminId,
  getProjectAdminById } from 'adminApp/reducer'

class ProjectAdminsContainer extends React.Component {

  componentWillReceiveProps(nextProps) {
    const oldProject = this.props.project
    const nextProject = nextProps.project
    if(nextProject && nextProject != oldProject) {
      const href = nextProject.links.find(l => l.rel === "users").href;
      this.props.fetchProjectAdminsBy(href)
    }
  }

  render () {
    return (
      <div>
        <ProjectAdminsComponent
          project={this.props.project}
          projectAdmins={this.props.projectAdmins}
          onAddClick={this.props.showProjectAdminConfiguration}
          onConfigureClick={this.props.showProjectAdminConfiguration}
          onDeleteClick={this.props.handleDelete}
          styles={this.props.styles} />
        <UserFormComponent
          projectAdmin={this.props.activeProjectAdmin}
          show={this.props.projectAdminConfigurationIsShown}
          onSubmit={this.props.onUserFormSubmit}
          onCancelClick={this.props.hideProjectAdminConfiguration}
          styles={this.props.styles} />
    </div>
    )
  }
}

ProjectAdminsContainer.propTypes = {
  projectAdmins: PropTypes.object
};
const mapStateToProps = (state, ownProps) => {
  const selectedProjectId = getSelectedProjectId(state)
  const selectedProject = getProjectById(state, selectedProjectId)
  const projectAdmins = getProjectAdmins(state) // TODO: By project: getProjectAdminsByProject(state, selectedProject)
  const selectedProjectAdminId = getSelectedProjectAdminId(state)
  const selectedProjectAdmin = getProjectAdminById(state, selectedProjectAdminId)
  return {
    project: selectedProject,
    projectAdmins: projectAdmins,
    projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
    selectedProjectAdmin: selectedProjectAdmin,
    styles: getThemeStyles(state.common.theme, 'adminApp/styles')
  }
}
const mapDispatchToProps = (dispatch) => ({
  fetchProjectAdminsBy: (href) => dispatch(fetchProjectAdminsBy(href)),
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
