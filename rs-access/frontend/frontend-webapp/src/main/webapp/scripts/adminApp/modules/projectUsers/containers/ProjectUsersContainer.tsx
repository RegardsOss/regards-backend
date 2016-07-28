import * as React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import {Card, CardActions, CardHeader, CardText} from 'material-ui/Card';
import FlatButton from 'material-ui/FlatButton';
import {List, ListItem} from 'material-ui/List';
import IconPeople from 'material-ui/svg-icons/social/people';
import { map } from 'lodash'

interface MenuButtonProps {
    styles: any,
    label: string,
    to?: string,
    icon: string,
    onClick?: () => void
}
interface ProjectUsersProps {
  users: any
}

class ProjectUsersContainer extends React.Component<any, ProjectUsersProps> {
  users: Array<any>;
  constructor(){
    super();
  }
  render () {
    const {users} = this.props;
    console.log(users)
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
          {map(this.users, (user: any, id: String) =>(
            <ListItem key={id} primaryText={user.name} leftIcon={<IconPeople />} />
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
