import * as React from "react"
import { I18nProvider } from "@regardsoss/i18n"
import { Connection } from "@regardsoss/models"
import ConnectionListComponent from "../components/list/ConnectionListComponent"
import { connect } from "react-redux"
import ConnectionSelectors from "../model/connection.selectors"

interface ConnectionListProps {
  // From router
  params: any

  // From mapStateToProps
  connections?: Array<Connection>
}


/**
 */
class ConnectionListContainer extends React.Component<ConnectionListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }

  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/connection/create"
  }

  render (): JSX.Element {
    const {connections} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ConnectionListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          connections={connections}
        />
      </I18nProvider>
    )
  }
}
const mapStateToProps = (state: any, ownProps: any) => {
  const connections = ConnectionSelectors.getConnections(state)
  return {
    connections
  }
}
export default connect<{}, {}, ConnectionListProps>(mapStateToProps, null)(ConnectionListContainer)
