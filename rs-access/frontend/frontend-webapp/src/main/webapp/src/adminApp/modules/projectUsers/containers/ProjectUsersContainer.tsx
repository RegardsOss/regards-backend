import * as React from "react"
import { connect } from "react-redux"
import { Card, CardHeader } from "material-ui/Card"
import { map } from "lodash"
import { User } from "../../../../common/users/types"
import ProjectUserContainer from "./ProjectUserContainer"
import Actions from "../actions"
import * as selectors from "../../../reducer"
import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn } from "material-ui/Table"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
const URL_PROJECTS_USERS = "http://localhost:8080/api/users"


interface ProjectUsersProps {
  // From mapStateToProps
  userLinks?: Array<User>,
  // From mapDispatchToProps
  fetchProjectUsers?: (urlProjectUsers: string) => void,
  deleteProjectUser?: (linkDeleteUser: string) => void,
  // From router
  router: any,
  route: any,
  params: any
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
    console.log("The state is now ", this.state)
    return (
      <I18nProvider messageDir='adminApp/modules/projectUsers/i18n'>
        <Card
          initiallyExpanded={true}>
          <CardHeader
            title={<FormattedMessage id="userlist.header"/>}
            actAsExpander={true}
            showExpandableButton={false}
          />
          <Table
            selectable={false}
            multiSelectable={false}
          >
            <TableHeader
              enableSelectAll={false}
              adjustForCheckbox={false}
              displaySelectAll={false}
            >
              <TableRow>
                <TableHeaderColumn><FormattedMessage id="userlist.login"/></TableHeaderColumn>
                <TableHeaderColumn><FormattedMessage id="userlist.firstName"/></TableHeaderColumn>
                <TableHeaderColumn><FormattedMessage id="userlist.lastName"/></TableHeaderColumn>
                <TableHeaderColumn><FormattedMessage id="userlist.email"/></TableHeaderColumn>
                <TableHeaderColumn><FormattedMessage id="userlist.status"/></TableHeaderColumn>
                <TableHeaderColumn><FormattedMessage id="userlist.action"/></TableHeaderColumn>
              </TableRow>
            </TableHeader>
            <TableBody displayRowCheckbox={false} preScanRows={false}>

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
      </I18nProvider>
    )
  }
}


const mapStateToProps = (state: any, ownProps: any) => {
  const userLinks = selectors.getUserLinks(state)
  return {
    userLinks: userLinks
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  fetchProjectUsers: (urlProjectUsers: string) => dispatch(Actions.fetchProjectUsers(urlProjectUsers))
})
export default connect<{}, {}, ProjectUsersProps>(mapStateToProps, mapDispatchToProps)(ProjectUsersContainer)
