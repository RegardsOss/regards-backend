import * as React from "react"
import { connect } from "react-redux"
import { User } from "../../../../common/users/types"
import ProjectUserComponent from "../components/ProjectUserComponent"
import * as selectors from "../../../reducer"
import { browserHistory } from "react-router"
import { find } from "lodash"
import Actions from "../actions"
import ProjectUserDeleteComponent from "../components/ProjectUserDeleteComponent"

interface ProjectUsersProps {
  projectName: string,
  userLink: string,
  // From mapStateToProps
  user?: User,
  // From mapDispatchToProps
  deleteProjectUser?: (linkDeleteUser: string) => void
}

/**
 * Show the list of users for the current project
 */
class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {

  state: any = {
    openDeleteDialog: false
  }

  generateUserProfileUrl = (user: User) => {
    return "/admin/" + this.props.projectName + "/users/" + user.accountId
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
    const user = this.props.user;
    const LINK_TYPE_DELETE = "role" // TODO: to change
    const userDeleteLink = find(user.links, {"rel": LINK_TYPE_DELETE})
    if (userDeleteLink) {
      this.props.deleteProjectUser(userDeleteLink.href)
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
    const user = this.props.user;
    const urlTo = "/admin/" + this.props.projectName + "/users/" + user.accountId;
    browserHistory.push(urlTo)
  }

  handleEdit = () => {
    const user = this.props.user;
    const urlTo = "/admin/" + this.props.projectName + "/users/" + user.accountId + "/edit";
    browserHistory.push(urlTo)
  }

  render (): JSX.Element {
    const {user} = this.props
    let dialog: JSX.Element
    if (this.state.openDeleteDialog) {
      dialog = <ProjectUserDeleteComponent
        onClose={this.handleCloseDeleteDialog}
        onDelete={this.handleDeleteUserDialog}
      />
    }
    return (
      <ProjectUserComponent
        user={user}
        handleView={this.handleView}
        handleEdit={this.handleEdit}
        handleDelete={this.handleDeleteUserDropdown}
        redirectOnSelectTo={this.generateUserProfileUrl(user)}
      >
        {dialog}
      </ ProjectUserComponent>
    )
  }
}


const mapStateToProps = (state: any, ownProps: any) => {
  const user = selectors.getUsersById(state, ownProps.userLink)
  return {
    user: user
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  deleteProjectUser: (linkDeleteUser: string) => dispatch(Actions.deleteProjectAdmin(linkDeleteUser))
})
export default connect<{}, {}, ProjectUsersProps>(mapStateToProps, mapDispatchToProps)(ProjectUsersContainer)
