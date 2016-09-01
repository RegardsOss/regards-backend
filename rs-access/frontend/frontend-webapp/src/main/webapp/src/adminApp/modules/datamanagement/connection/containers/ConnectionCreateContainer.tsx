import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { browserHistory } from "react-router"
import ConnectionCreateComponent from "../components/add/ConnectionCreateComponent"


/**
 */
interface ConnectionCreateProps {
  // From router
  params: any
  // From mapDispatchToProps
  addConnection?: (id: number, name: string) => void
}
export default class ConnectionCreateContainer extends React.Component<ConnectionCreateProps, any> {

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

  handleNextStep = (name: string) => {
    const id = Math.floor(Math.random() * 60) + 10
    this.props.addConnection(id, name)
    browserHistory.push(this.getCancelUrl())
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ConnectionCreateComponent
          getCancelUrl={this.getCancelUrl}
          handleNextStep={this.handleNextStep}
        />
      </I18nProvider>
    )
  }
}
/*
 const mapStateToProps = (state: any, ownProps: any) => {
 const viewState = Selectors.getFormViewState(state)
 return {
 viewState: viewState
 }
 }
 const mapDispatchToProps = (dispatch: any) => ({
 setViewState: (newState: string) => dispatch(Actions.setViewState(newState))
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
