import * as React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import {Card, CardActions, CardHeader, CardText} from 'material-ui/Card';
import FlatButton from 'material-ui/FlatButton';
import {List} from 'material-ui/List';
import IconPeople from 'material-ui/svg-icons/social/people';
import { map } from 'lodash'
import {User} from '../../../../common/users/types'

import ProjectUserComponent from '../components/ProjectUserComponent';
interface ProjectUsersProps {
  users: Array<User>,
  // From router
    router: any,
    route : any,
    params: any,
}

class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {
  constructor(props: any){
    super(props);
  }

  generateUserEditUrl = (user: User) => {
    return "/admin/"+this.props.params.project+"/users/"+user.id;
  }

  render () {

    const {users, params} = this.props;
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
          {map(users, (user: User, id: String) =>(
            <ProjectUserComponent
              user={user}
              key={user.id}
              redirectOnSelectTo={this.generateUserEditUrl(user)}
            />
          ))}
        </List>
        <CardActions >
          <FlatButton label="Add user" />
          <FlatButton label="Remove user" />
        </CardActions>
      </Card>
    )
  }
}

const mapStateToProps = (state: any) => ({
});
const mapDispatchToProps = (dispatch: any) => ({
});
export default connect<{}, {}, ProjectUsersProps>(mapStateToProps, mapDispatchToProps)(ProjectUsersContainer);
