import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import * as Selectors from "../../../../reducer"
import { connect } from "react-redux"
import { DatasourceModel } from "../DatasourceModel"
import ModelListComponent from "../../datasetmodel/components/DatasetModelListComponent"
import DatasourceModelListComponent from "../components/DatasourceModelListComponent"
/**
 */
interface ModelListProps {
  // From router
  params: any
  // From mapStateToProps
  datasourceModels?: Array<DatasourceModel>
}

export class DatasourceModelListContainer extends React.Component<ModelListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasetmodel/create"
  }

  render (): JSX.Element {
    const {datasourceModels} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <DatasourceModelListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          datasourceModels={datasourceModels}
        />
      </I18nProvider>
    )
  }
}
const mapStateToProps = (state: any, ownProps: any) => {
  const datasourceModels = Selectors.getDatasourceModels(state)
  return {
    datasourceModels
  }
}
export default connect<{}, {}, ModelListProps>(mapStateToProps, null)(DatasourceModelListContainer)
