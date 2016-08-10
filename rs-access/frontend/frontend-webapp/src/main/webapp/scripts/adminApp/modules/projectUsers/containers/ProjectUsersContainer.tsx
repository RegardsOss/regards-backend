import * as React from "react"
import { connect } from "react-redux"
import { Card, CardHeader } from "material-ui/Card"
import { map } from "lodash"
import { User } from "../../../../common/users/types"
import ProjectUserContainer from "./ProjectUserContainer"
import Actions from "../actions"
import * as selectors from "../../../reducer"
import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow } from "material-ui/Table"
const URL_PROJECTS_USERS = "http://localhost:8080/api/users"

interface ProjectUsersProps {
  // From mapStateToProps
  userLinks?: Array<User>,
  // From mapDispatchToProps
  fetchProjectUsers?: any,
  // From router
  router: any,
  route: any,
  params: any,
}

/**
 * Show the list of users for the current project
 */
class ProjectUsersContainer extends React.Component<ProjectUsersProps, any> {
  constructor (props: any) {
    super(props)
    // Fetch users for the current project when the container is created
    this.props.fetchProjectUsers(URL_PROJECTS_USERS)
  }

  render (): JSX.Element {

    const {userLinks, params} = this.props
    return (
      <Card
        initiallyExpanded={true}
      >
        <CardHeader
          title="User list"
          actAsExpander={true}
          showExpandableButton={false}
        />
        <Table>
          <TableHeader>
            <TableRow>
              <TableHeaderColumn>HTTP Verb</TableHeaderColumn>
              <TableHeaderColumn>Route Name</TableHeaderColumn>
              <TableHeaderColumn>Access right</TableHeaderColumn>
              <TableHeaderColumn>HTTP Verb</TableHeaderColumn>
              <TableHeaderColumn>Route Name</TableHeaderColumn>
            </TableRow>
          </TableHeader>
          <TableBody>
            {map(userLinks, (userLink: string, id: string) => (
              <ProjectUserContainer
                userLink={userLink}
                projectName={params.project}
                key={id}
              />
            ))}
          </TableBody>
        </Table>
      </Card>
    )
  }


  componentWillReceiveProps (nextProps: any): any {
    /*
     if (nextProject && nextProject !== oldProject) {
     const link = nextProject.links.find((link: any) => link.rel === "users")
     if (link) {
     const href = link.href;
     this.props.fetchProjectUsers(href)
     }
     }*/
  }
}


const mapStateToProps = (state: any, ownProps: any) => {
  const userLinks = selectors.getUserLinks(state)
  return {
    userLinks: userLinks
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  fetchProjectUsers: (urlProjectUsers: string) => dispatch(Actions.fetchProjectUsers(urlProjectUsers)),
})
export default connect<{}, {}, ProjectUsersProps>(mapStateToProps, mapDispatchToProps)(ProjectUsersContainer)
