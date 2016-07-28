import * as React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import {Card, CardActions, CardHeader, CardText} from 'material-ui/Card';
import FlatButton from 'material-ui/FlatButton';
import {List, ListItem} from 'material-ui/List';
import ContentInbox from 'material-ui/svg-icons/content/inbox';

class ProjectUserEditContainer extends React.Component<any, any> {
  constructor(){
    super();
  }
  render () {
    return (
      <Card
        initiallyExpanded={true}
      >
        <CardHeader
          title="User rights"
          subtitle="You can overide group rights by settings user rights for each projects."
          actAsExpander={true}
          showExpandableButton={true}
        />
        <CardActions >
          <FlatButton label="Add user" />
          <FlatButton label="Remove user" />
        </CardActions>
      </Card>
    )
  }
}
// export default ProjectUserEditContainer;
const mapStateToProps = (state: any) => ({
});
const mapDispatchToProps = (dispatch: any) => ({

});
export default connect<{}, {}, any>(mapStateToProps, mapDispatchToProps)(ProjectUserEditContainer);
