import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import * as Selectors from "../../../../reducer"
import { connect } from "react-redux"
import { DatasourceModel } from "../DatasourceModel"
import DatasourceModelListComponent from "../components/DatasourceModelListComponent"
/**
 */
interface DatasourceModelListProps {
  // From router
  params: any
  // From mapStateToProps
  datasourceModels?: Array<DatasourceModel>
}

class DatasourceModelListContainer extends React.Component<DatasourceModelListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasourcemodel/create"
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
export default connect<{}, {}, DatasourceModelListProps>(mapStateToProps, null)(DatasourceModelListContainer)
