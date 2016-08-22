import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import PickModelFormComponent from "../components/add/PickModelFormComponent"
import StepperCreateDatasetComponent from "../components/add/StepperCreateDatasetComponent"
import PickDatasourceFormComponent from "../components/add/PickDatasourceFormComponent"
import CreateDatasetSuccessComponent from "../components/add/CreateDatasetSuccessComponent"
import { connect } from "react-redux"
import * as Selectors from "../../../../reducer"
import * as Actions from "../DatasetActions"
import { browserHistory } from "react-router"


export const STATES = {
  SELECT_MODELE: "select_modele",
  SELECT_SOURCE: "select_source",
  DONE: "done",
}


interface DatasetCreateProps {
  // From mapStateToProps
  viewState?: string,
  // From router
  router: any,
  route: any,
  params: any,
  location: any,

  // From mapDispatchToProps
  setViewState?: (newState: string) => void
}
/**
 */
export class DatasetCreateContainer extends React.Component<DatasetCreateProps, any> {

  handleNextStep = () => {
    switch (this.props.viewState) {
      case STATES.SELECT_MODELE:
        this.props.setViewState(STATES.SELECT_SOURCE)
        break
      case STATES.SELECT_SOURCE:
        this.props.setViewState(STATES.DONE)
        break
      case STATES.DONE:
        const urlTo = "/admin/" + this.props.params.projectName + "/datamanagement/"
        browserHistory.push(urlTo)
        break
      default:
        throw 'Undefined state ' + this.props.viewState
    }
  }

  getStepperIndex = () => {
    switch (this.props.viewState) {
      case STATES.SELECT_MODELE:
        return 0
      case STATES.SELECT_SOURCE:
        return 1
      case STATES.DONE:
        return 2
      default:
        throw 'Undefined state ' + this.props.viewState
    }
  }

  handleNewModel = () => {
    const from = encodeURIComponent(this.props.location.pathname)
    console.log(this.props.params)
    const urlTo = "/admin/" + this.props.params.project + "/model/" + from
    browserHistory.push(urlTo)
  }

  render (): JSX.Element {
    const {viewState} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <StepperCreateDatasetComponent
            getStepperIndex={this.getStepperIndex}
          />
          {(() => {
            switch (viewState) {
              case STATES.SELECT_MODELE:
                return <PickModelFormComponent
                  handleNextStep={this.handleNextStep}
                  handleNewModel={this.handleNewModel}
                />
              case STATES.SELECT_SOURCE:
                return <PickDatasourceFormComponent
                  handleNextStep={this.handleNextStep}
                />
              case STATES.DONE:
                return <CreateDatasetSuccessComponent
                  handleNextStep={this.handleNextStep}
                />
              default:
                throw 'Undefined state ' + viewState
            }
          })()}
        </div>
      </I18nProvider>
    )
  }
}
/*
 previously => (

 <hr />
 <CreateModelFormComponent />
 <hr />
 <CreateDatasourceFormComponent />
 <hr />
 <CreateConnectionFormComponent />
 )*/
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
