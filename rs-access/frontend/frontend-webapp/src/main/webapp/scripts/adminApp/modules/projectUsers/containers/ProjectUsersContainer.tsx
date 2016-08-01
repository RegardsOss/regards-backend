import * as React from "react";
import { connect } from "react-redux";
import { Card, CardActions, CardHeader } from "material-ui/Card";
import FlatButton from "material-ui/FlatButton";
import { List } from "material-ui/List";
import { map } from "lodash";
import { User } from "../../../../common/users/types";
import ProjectUserComponent from "../components/ProjectUserComponent";
interface ProjectUsersProps {
  users: Array<User>,
  // From router
  router: any,
  route: any,
  params: any,
}

class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {
  constructor(props: any) {
    super (props);
  }

  generateUserEditUrl = (user: User) => {
    return "/admin/" + this.props.params.project + "/users/" + user.id;
  }

  render(): any {

    const {users} = this.props;
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
}

const mapStateToProps = (state: any) => ({});
const mapDispatchToProps = (dispatch: any) => ({});
export default connect<{}, {}, ProjectUsersProps> (mapStateToProps, mapDispatchToProps) (ProjectUsersContainer);
