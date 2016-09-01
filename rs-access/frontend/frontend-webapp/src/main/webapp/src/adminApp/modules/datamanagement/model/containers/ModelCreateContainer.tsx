import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { connect } from "react-redux"
import { ModelAttribute } from "../ModelAttribute"
import { addModel } from "../actions"
import ModelCreateComponent from "../components/add/ModelCreateComponent"
import { browserHistory } from "react-router"

/**
 */
interface ModelCreateProps {
  // From router
  params: any
  // From mapDispatchToProps
  addModel?: (id: number, name: string, attributes: Array<ModelAttribute>) => void
}
export class ModelCreateContainer extends React.Component<ModelCreateProps, any> {

  getCancelUrl = () => {
    const from = this.props.params.from
    if (from) {
      const fromURI = decodeURIComponent(from)
      return fromURI
    } else {
      const projectName = this.props.params.project
      return "/admin/" + projectName + "/datamanagement/model"
    }
  }

  handleNextStep = (name: string, attributes: Array<ModelAttribute>) => {
    const id = Math.floor(Math.random() * 60) + 10
    this.props.addModel(id, name, attributes)
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
  addModel: (id: number, name: string, attributes: Array<ModelAttribute>) => dispatch(addModel(id, name, attributes)),
})
export default connect<{}, {}, ModelCreateProps>(null, mapDispatchToProps)(ModelCreateContainer)
