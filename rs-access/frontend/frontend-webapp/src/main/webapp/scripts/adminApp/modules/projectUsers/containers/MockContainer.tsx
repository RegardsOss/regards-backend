import * as React from 'react'
import { connect } from 'react-redux'
import ProjectUsersContainer from './ProjectUsersContainer'
import ProjectUserEditContainer from './ProjectUserEditContainer'
import ProjectUserCreateContainer from './ProjectUserCreateContainer'

class MockContainer extends React.Component<any, any> {
  context: any;
  static contextTypes: {
    muiTheme: Object
  }
  constructor(){
    super();
  }
  render () {
    console.log("The context is ", this.context)
    const userList = [{
      name: "Eric"
    }, {
      name: "Joseph"
    }, {
      name: "Martin"
    }, {
      name: "John doe"
    }]
    return (
      <div>
        <ProjectUsersContainer users={userList} />
        <ProjectUserEditContainer />
        <ProjectUserCreateContainer />
      </div>
    )
  }
}
const mapStateToProps = (state: any) => ({
});
const mapDispatchToProps = (dispatch: any) => ({

});
export default MockContainer;
