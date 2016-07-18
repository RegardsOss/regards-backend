/** @module AdminProjectAdmins */
import * as React from 'react'
import { PropTypes } from "react"
import { connect } from 'react-redux';
// Containers
import UserFormContainer from './UserFormContainer'
// Components
import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import ProjectAdminsComponent from '../components/ProjectAdminsComponent'
// Actions
import * as actions from '../actions'
import * as uiActions from '../../ui/actions'
// Selectors
import * as selectors from '../../../reducer'
// Styles
var classnames = require('classnames')
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'

interface ProjectAdminsProps {
  // Properties set by react-redux connection
  activeProjectAdmin?: any,
  projectAdminConfigurationIsShown?: any,
  onUserFormSubmit?: () => void,
  hideProjectAdminConfiguration?: () => void,
  styles?: any,
  project?: any,
  projectAdmins?: Array<any>,
  showProjectAdminConfiguration?: any,
  fetchProjectAdminsBy?: any,
  handleDelete?:any
}


/**
 * React container to manage ProjectAdminsComponent.
 */
class ProjectAdminsContainer extends React.Component<ProjectAdminsProps, any> {

  componentWillReceiveProps(nextProps: any) {
    const oldProject = this.props.project
    const nextProject = nextProps.project
    if(nextProject && nextProject != oldProject) {
      const link = nextProject.links.find( (link:any) => link.rel === "users")
      if(link) {
        const href = link.href;
        this.props.fetchProjectAdminsBy(href)
      }
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

        <UserFormContainer
          show={this.props.projectAdminConfigurationIsShown}
          handleSubmit={this.props.onUserFormSubmit}
          onCancelClick={this.props.hideProjectAdminConfiguration}
          styles={this.props.styles} />
    </div>
    )
  }
}

const mapStateToProps = (state: any, ownProps: any) => {
  const selectedProjectId = selectors.getSelectedProjectId(state)
  const selectedProject = selectors.getProjectById(state, selectedProjectId)
  const projectAdmins = selectors.getProjectAdmins(state) // TODO: By project: getProjectAdminsByProject(state, selectedProject)
  const selectedProjectAdminId = selectors.getSelectedProjectAdminId(state)
  const selectedProjectAdmin = selectors.getProjectAdminById(state, selectedProjectAdminId)
  return {
    project: selectedProject,
    projectAdmins: projectAdmins,
    projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
    selectedProjectAdmin: selectedProjectAdmin,
    styles: getThemeStyles(state.common.theme, 'adminApp/styles')
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  fetchProjectAdminsBy: (href: any) => dispatch(actions.fetchProjectAdminsBy(href)),
  showProjectAdminConfiguration: (id: string) => {
    dispatch(uiActions.selectProjectAdmin(id))
    dispatch(uiActions.showProjectAdminConfiguration())
  },
  hideProjectAdminConfiguration: () => dispatch(uiActions.hideProjectAdminConfiguration()),
  onUserFormSubmit: (e: any) => {
    dispatch(actions.updateOrCreateProjectAdmin(e.id, e.username, e.projectId))
    dispatch(uiActions.hideProjectAdminConfiguration())
  },
  handleDelete: (id: string) => {
    dispatch(actions.deleteProjectAdmin(id))
    dispatch(uiActions.hideProjectAdminConfiguration())
  }
})
export default connect<{}, {}, ProjectAdminsProps>(mapStateToProps, mapDispatchToProps)(ProjectAdminsContainer);
