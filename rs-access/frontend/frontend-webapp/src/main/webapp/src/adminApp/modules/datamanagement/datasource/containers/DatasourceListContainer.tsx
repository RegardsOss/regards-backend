import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { Datasource } from "../Datasource"
import DatasourceListComponent from "../components/list/DatasourceListComponent"
import * as Selectors from "../../../../reducer"
import { connect } from "react-redux"


interface DatasourceListProps {
  // From router
  params: any

  // From mapStateToProps
  datasources?: Array<Datasource>
}


/**
 */
class DatasourceListContainer extends React.Component<DatasourceListProps, any> {


  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }

  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasource/create"
  }

  render (): JSX.Element {
    const {datasources} = this.props
    console.log(datasources)
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <DatasourceListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          datasources={datasources}
        />
      </I18nProvider>
    )
  }
}
const mapStateToProps = (state: any, ownProps: any) => {
  const datasources = Selectors.getDatasources(state)
  return {
    datasources
  }
}
export default connect<{}, {}, DatasourceListProps>(mapStateToProps, null)(DatasourceListContainer)
