import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { connect } from "react-redux"
import * as Selectors from "../../../../reducer"
import { Dataset } from "../Dataset"
import DatasetListComponent from "../components/list/DatasetListComponent"
const URL_PROJECTS_USERS = "http://localhost:8080/api/users"

interface DatasetCreateProps {
  // From router
  params: any

  // From mapStateToProps
  datasets: Array<Dataset>
}

/**
 * Show the list of users for the current project
 */
export class DatasetListContainer extends React.Component<DatasetCreateProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/dataset/create"
  }

  render (): JSX.Element {
    const {datasets} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <DatasetListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          datasets={datasets}
        />
      </I18nProvider>
    )
  }
}


const mapStateToProps = (state: any, ownProps: any) => {
  const datasets = Selectors.getDatasets(state)
  return {
    datasets
  }
}
const mapDispatchToProps = (dispatch: any) => ({})
export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetListContainer)


