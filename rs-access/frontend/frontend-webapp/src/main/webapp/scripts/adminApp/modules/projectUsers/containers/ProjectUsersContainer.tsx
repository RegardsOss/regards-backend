import * as React from "react"
import { connect } from "react-redux"
import { Card, CardActions, CardHeader } from "material-ui/Card"
import FlatButton from "material-ui/FlatButton"
import { List } from "material-ui/List"
import { map } from "lodash"
import { User } from "../../../../common/users/types"
import ProjectUserComponent from "../components/ProjectUserComponent"
interface ProjectUsersProps {
  users: Array<User>,
  // From mapDispatchToProps
  fetchProjectUsers?: any,
  // From router
  router: any,
  route: any,
  params: any,
}

class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {
  constructor(props: any) {
    super (props)
  }

  generateUserEditUrl = (user: User) => {
    return "/admin/" + this.props.params.project + "/users/" + user.id
  }

  render(): JSX.Element {

    const {users} = this.props
    return (
      <Card
        initiallyExpanded={true}
      >
        <CardHeader
          title="User list"
          actAsExpander={true}
          showExpandableButton={true}
        />
        <List>
          {map (users, (user: User, id: String) => (
            <ProjectUserComponent
              user={user}
              key={user.id}
              redirectOnSelectTo={this.generateUserEditUrl(user)}
            />
          ))}
        </List>
        <CardActions >
          <FlatButton label="Add user"/>
          <FlatButton label="Remove user"/>
        </CardActions>
      </Card>
    )
  }


  componentWillReceiveProps(nextProps: any): any {
    /*
    const oldProject = this.props.project
    const nextProject = nextProps.project
    if (nextProject && nextProject !== oldProject) {
      const link = nextProject.links.find ((link: any) => link.rel === "users")
      if (link) {
        const href = link.href;
        this.props.fetchProjectUsers (href)
      }
    }*/
  }
}

const mapStateToProps = (state: any) => ({})
const mapDispatchToProps = (dispatch: any) => ({
  // fetchProjectUsers: (projectId: string) => dispatch (actions.fetchProjectUsers (projectId)),
})
export default connect<{}, {}, ProjectUsersProps> (mapStateToProps, mapDispatchToProps) (ProjectUsersContainer)
