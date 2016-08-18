import * as React from "react"
import { connect } from "react-redux"
import { ProjectAccount } from "../../../../common/models/users/types"
import ProjectAccountComponent from "../components/ProjectAccountComponent"
import * as selectors from "../../../reducer"
import { browserHistory } from "react-router"
import { find } from "lodash"
import Actions from "../actions"
import ProjectAccountDeleteComponent from "../components/ProjectAccountDeleteComponent"

interface ProjectAccountProps {
  projectName: string,
  // From mapStateToProps
  projectAccount?: ProjectAccount,
  // From mapDispatchToProps
  deleteProjectAccount?: (linkDeleteProjectAccount: string) => void
}

/**
 * Show the list of users for the current project
 */
class ProjectAccountContainer extends React.Component<ProjectAccountProps, any> {

  state: any = {
    openDeleteDialog: false
  }

  generateUserProfileUrl = (projectAccount: ProjectAccount) => {
    return "/admin/" + this.props.projectName + "/users/" + projectAccount.account.accountId
  }

  /**
   *
   * @param user
   */
  handleDeleteUserDropdown = () => {
    this.setState({
      openDeleteDialog: true
    })
  }

  deleteUser = () => {
    const user = this.props.projectAccount
    const LINK_TYPE_DELETE = "role" // TODO: to change
    const userDeleteLink = find(user.links, {"rel": LINK_TYPE_DELETE})
    if (userDeleteLink) {
      this.props.deleteProjectAccount(userDeleteLink.href)
    } else {
      throw new Error("insufficient permission")
      // TODO: How to display to the user he does not have the right to delete somebody else ?
    }
  }

  handleDeleteUserDialog = () => {
    this.deleteUser()
    this.handleCloseDeleteDialog()
  }

  /**
   *
   */
  handleCloseDeleteDialog = () => {
    this.setState({
      openDeleteDialog: false
    })
  }

  handleView = () => {
    const user = this.props.projectAccount
    const urlTo = "/admin/" + this.props.projectName + "/users/" + user.account.accountId;
    browserHistory.push(urlTo)
  }

  handleEdit = () => {
    const user = this.props.projectAccount;
    const urlTo = "/admin/" + this.props.projectName + "/users/" + user.account.accountId + "/edit"
    browserHistory.push(urlTo)
  }

  render (): JSX.Element {
    const {projectAccount} = this.props
    let dialog: JSX.Element
    if (this.state.openDeleteDialog) {
      dialog = <ProjectAccountDeleteComponent
        onClose={this.handleCloseDeleteDialog}
        onDelete={this.handleDeleteUserDialog}
      />
    }
    return (
      <ProjectAccountComponent
        projectAccount={projectAccount}
        handleView={this.handleView}
        handleEdit={this.handleEdit}
        handleDelete={this.handleDeleteUserDropdown}
        redirectOnSelectTo={this.generateUserProfileUrl(projectAccount)}
      >
        {dialog}
      </ProjectAccountComponent>
    )
  }
}


const mapStateToProps = (state: any, ownProps: ProjectAccountProps) => {
  const user = selectors.getProjectAccountById(state, ownProps.projectAccount.account.accountId)
  return {
    user: user
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  deleteUser: (linkDeleteUser: string) => dispatch(Actions.deleteUser(linkDeleteUser))
})
export default connect<{}, {}, ProjectAccountProps>(mapStateToProps, mapDispatchToProps)(ProjectAccountContainer)
