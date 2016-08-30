import * as React from "react"
import { connect } from "react-redux"
import { Card, CardHeader } from "material-ui/Card"
import { map } from "lodash"
import { ProjectAccount } from "../../../../common/models/users/types"
import ProjectAccountContainer from "./ProjectAccountContainer"
import Actions from "../actions"
import * as selectors from "../../../reducer"
import { Table, TableBody, TableHeader, TableHeaderColumn, TableRow } from "material-ui/Table"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
const URL_PROJECTS_ACCOUNTS = "http://localhost:8080/api/projectAccounts"


interface ProjectAccountsProps {
  // From mapStateToProps
  projectAccounts?: Array<ProjectAccount>,
  // From mapDispatchToProps
  fetchProjectAccounts?: (urlProjectAccounts: string) => void,
  deleteProjectAccount?: (linkDeleteProjectAccount: string) => void,
  // From router
  router: any,
  route: any,
  params: any
}

/**
 * Show the list of users for the current project
 */
class ProjectAcountsContainer extends React.Component<ProjectAccountsProps, any> {


  constructor (props: any) {
    super(props)
    // Fetch users for the current project when the container is created
    this.props.fetchProjectAccounts(URL_PROJECTS_ACCOUNTS)
  }

  render (): JSX.Element {

    const {projectAccounts, params} = this.props
    console.log("The state is now ", this.state)
    console.log("SEB", this.props.projectAccounts)
    return (
      <I18nProvider messageDir='adminApp/modules/userManagement/i18n'>
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

              {map(projectAccounts, (projectAccount: ProjectAccount, id: string) => (
                <ProjectAccountContainer
                  projectAccount={projectAccount}
                  projectName={params.project}
                  key={projectAccount.projectAccountId}
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
  const projectAccounts = selectors.getProjectAccountsId(state)
  return {
    projectAccounts: projectAccounts
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  fetchProjectAccounts: (urlProjectAccounts: string) => dispatch(Actions.fetchProjectAccounts(urlProjectAccounts))
})
export default connect<{}, {}, ProjectAccountsProps>(mapStateToProps, mapDispatchToProps)(ProjectAcountsContainer)
