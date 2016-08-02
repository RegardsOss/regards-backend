import * as React from "react";
import ProjectUsersContainer from "./ProjectUsersContainer";
import ProjectUserEditContainer from "./ProjectUserEditContainer";
import ProjectUserCreateContainer from "./ProjectUserCreateContainer";

interface MockProps {
  // From router
  router: any,
  route: any,
  params: any,
}

class MockContainer extends React.Component<any, MockProps> {
  static contextTypes: {
    muiTheme: Object
  };
  context: any;
  constructor() {
    super ();
  }
  render(): JSX.Element {
    const {router, route, params} = this.props;
    const userList = [{
      name: "Eric",
      id: "1"
    }, {
      name: "Joseph",
      id: "10"
    }, {
      name: "Martin",
      id: "100"
    }, {
      name: "John doe",
      id: "1000"
    }]
    return (
      <div>
        <ProjectUsersContainer users={userList} router={router} route={route} params={params}/>
        <ProjectUserEditContainer />
        <ProjectUserCreateContainer />
      </div>
    )
  }
}
export default MockContainer;
