import * as React from "react"
import { connect } from "react-redux"
import { User } from "../../../../common/users/types"
import ProjectUserComponent from "../components/ProjectUserComponent"
import * as selectors from "../../../reducer"
const URL_PROJECTS_USERS = "http://localhost:8080/api/users"

interface ProjectUsersProps {
  projectName: string,
  userLink: string,
  // From mapStateToProps
  user?: User
}

/**
 * Show the list of users for the current project
 */
class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {

  generateUserProfileUrl = (user: User) => {
    return "/admin/" + this.props.projectName + "/users/" + user.accountId
  }

  render (): JSX.Element {
    const {user} = this.props
    console.log(user)
    return (
      <ProjectUserComponent
        user={user}
        redirectOnSelectTo={this.generateUserProfileUrl(user)}
      />
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

})
export default connect<{}, {}, ProjectUsersProps>(mapStateToProps, mapDispatchToProps)(ProjectUsersContainer)
