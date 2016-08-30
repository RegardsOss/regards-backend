import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import ModelListComponent from "../components/ModelListComponent"
import * as Selectors from "../../../../reducer"
import { connect } from "react-redux"
import { Model } from "../Model"
/**
 */
interface ModelListProps {
  // From router
  params: any
  // From mapStateToProps
  models?: Array<Model>
}
export class ModelListContainer extends React.Component<ModelListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/model/create"
  }

  render (): JSX.Element {
    const {models} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ModelListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          models={models}
        />
      </I18nProvider>
    )
  }
}
const mapStateToProps = (state: any, ownProps: any) => {
  const models = Selectors.getModels(state)
  return {
    models
  }
}
export default connect<{}, {}, ModelListProps>(mapStateToProps, null)(ModelListContainer)
