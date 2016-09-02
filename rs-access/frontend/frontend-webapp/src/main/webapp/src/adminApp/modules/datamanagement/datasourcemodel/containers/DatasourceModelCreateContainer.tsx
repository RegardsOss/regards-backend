import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { connect } from "react-redux"
import { ModelAttribute } from "../../datasetmodel/ModelAttribute"
import { addDatasourceModel } from "../actions"
import ModelCreateComponent from "../components/add/DatasourceModelCreateComponent"
import { browserHistory } from "react-router"

/**
 */
interface ModelCreateProps {
  // From router
  params: any
  // From mapDispatchToProps
  addDatasourceModel?: (name: string, attributes: Array<ModelAttribute>) => void
}

class DatasourceModelCreateContainer extends React.Component<ModelCreateProps, any> {

  getCancelUrl = () => {
    const from = this.props.params.from
    if (from) {
      const fromURI = decodeURIComponent(from)
      return fromURI
    } else {
      const projectName = this.props.params.project
      return "/admin/" + projectName + "/datamanagement/datasourcemodel"
    }
  }

  handleNextStep = (name: string, attributes: Array<ModelAttribute>) => {
    this.props.addDatasourceModel(name, attributes)
    browserHistory.push(this.getCancelUrl())
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ModelCreateComponent
          getCancelUrl={this.getCancelUrl}
          handleNextStep={this.handleNextStep}
        />
      </I18nProvider>
    )
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  addDatasourceModel: (name: string, attributes: Array<ModelAttribute>) => dispatch(addDatasourceModel(name, attributes)),
})
export default connect<{}, {}, ModelCreateProps>(null, mapDispatchToProps)(DatasourceModelCreateContainer)
