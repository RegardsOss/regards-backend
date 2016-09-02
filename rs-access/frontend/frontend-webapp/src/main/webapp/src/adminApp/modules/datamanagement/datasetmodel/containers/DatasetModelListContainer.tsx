import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import ModelListComponent from "../components/DatasetModelListComponent"
import * as Selectors from "../../../../reducer"
import { connect } from "react-redux"
import { DatasetModel } from "../DatasetModel"
/**
 */
interface ModelListProps {
  // From router
  params: any
  // From mapStateToProps
  datasetModels?: Array<DatasetModel>
}
export class ModelListContainer extends React.Component<ModelListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasetmodel/create"
  }

  render (): JSX.Element {
    const {datasetModels} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ModelListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          datasetModels={datasetModels}
        />
      </I18nProvider>
    )
  }
}
const mapStateToProps = (state: any, ownProps: any) => {
  const datasetModels = Selectors.getDatasetModels(state)
  return {
    datasetModels
  }
}
export default connect<{}, {}, ModelListProps>(mapStateToProps, null)(ModelListContainer)
