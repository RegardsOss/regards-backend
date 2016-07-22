/** @module AdminProjectAdmins */
import * as React from 'react'
import { PropTypes } from "react"
import { connect } from 'react-redux';
// Types
import { ProjectAdmin } from '../types'
// Containers
// import UserFormContainer from './UserFormContainer'
// Components
import AccessRightsComponent from '../../../../common/access-rights/AccessRightsComponent'
import UserList from '../../../../common/users/components/UserList'
import MenuItem from 'material-ui/MenuItem'
import Build from 'material-ui/svg-icons/action/build'
import Delete from 'material-ui/svg-icons/action/delete'
import EditUserDialog from '../../../../common/users/components/EditUserDialog'
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
  updateOrCreateProjectAdmin?: (id: string, payload: ProjectAdmin) => void,
  hideProjectAdminConfiguration?: () => void,
  styles?: any,
  project?: any,
  projectAdmins?: Array<any>,
  showProjectAdminConfiguration?: any,
  fetchProjectAdminsBy?: any,
  deleteProjectAdmin?:any,
  selectedProjectAdminId?: string
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

  state = {
    dialogOpen: false
  }

  handleEditClick = (event: Object) => {
    this.props.showProjectAdminConfiguration(this.props.selectedProjectAdminId)
  }

  handleDeleteClick = (event: Object) => {
    this.props.deleteProjectAdmin(this.props.selectedProjectAdminId)
  }

  handleDialogOpen = () => {
    this.setState({dialogOpen: true})
  }

  handleDialogClose = () => {
    this.setState({dialogOpen: false})
  }

  handleDialogSave = () => {
    this.handleDialogClose()
    this.props.updateOrCreateProjectAdmin('9999', {name: 'Fake Name'})
  }

  render () {
    const usersListMenuElements = [
      <MenuItem key={1} primaryText="Edit" leftIcon={<Build />} onTouchTap={this.handleDialogOpen} />,
      <MenuItem key={2} primaryText="Delete" leftIcon={<Delete />} onTouchTap={this.handleDeleteClick} />
    ]

    return (
      <div>
        <UserList
          subheader='Project administrators'
          items={this.props.projectAdmins}
          menuElements={usersListMenuElements}
          />
          <EditUserDialog
            open={this.state.dialogOpen}
            onClose={this.handleDialogClose}
            onSave={this.handleDialogSave}
          />
      </div>
    )
  }
}
// <UserFormContainer
// show={this.props.projectAdminConfigurationIsShown}
// handleSubmit={this.props.onUserFormSubmit}
// onSubmit={this.props.onUserFormSubmit}
// onCancelClick={this.props.hideProjectAdminConfiguration}
// styles={this.props.styles} />
const mapStateToProps = (state: any, ownProps: any) => {
  const selectedProjectId = selectors.getSelectedProjectId(state)
  const selectedProject = selectors.getProjectById(state, selectedProjectId)
  const projectAdmins = selectors.getProjectAdmins(state) // TODO: By project: getProjectAdminsByProject(state, selectedProject)
  const selectedProjectAdminId = selectors.getSelectedProjectAdminId(state)
  const selectedProjectAdmin = selectors.getProjectAdminById(state, selectedProjectAdminId)
  return {
    project: selectedProject,
    projectAdmins: projectAdmins.items,
    projectAdminConfigurationIsShown: state.adminApp.ui.projectAdminConfigurationIsShown,
    selectedProjectAdminId: selectedProjectAdminId,
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
  updateOrCreateProjectAdmin: (id: string, payload: ProjectAdmin) => dispatch(actions.updateOrCreateProjectAdmin(id, payload)),
  onUserFormSubmit: (e: any) => {
    dispatch(actions.updateOrCreateProjectAdmin(e.id, { name: e.username }))
    dispatch(uiActions.hideProjectAdminConfiguration())
  },
  deleteProjectAdmin: (id: string) => {
    dispatch(actions.deleteProjectAdmin(id))
    dispatch(uiActions.hideProjectAdminConfiguration())
  }
})
export default connect<{}, {}, ProjectAdminsProps>(mapStateToProps, mapDispatchToProps)(ProjectAdminsContainer);
