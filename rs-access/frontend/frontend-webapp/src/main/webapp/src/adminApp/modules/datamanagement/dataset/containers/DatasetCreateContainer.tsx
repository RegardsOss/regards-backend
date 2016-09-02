import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import PickModelFormComponent from "../components/add/pick_model/FormComponent"
import StepperCreateDatasetComponent from "../components/add/StepperCreateDatasetComponent"
import PickDatasourceFormComponent from "../components/add/pick_datasource/FormComponent"
import CreateDatasetSuccessComponent from "../components/add/CreateDatasetSuccessComponent"
import { connect } from "react-redux"
import * as Selectors from "../../../../reducer"
import * as Actions from "../formActions"
import { browserHistory } from "react-router"
import { DatasetModel } from "../../datasetmodel/DatasetModel"
import { DatasetDefaultModelAttribute } from "../DatasetDefaultModelAttribute"


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
  setDatasetLabel: (label: string) => void
  setDatasetModelType: (modelType: number) => void
  setDatasetDefaultModelAttributes: (attributesDefined: Array<DatasetDefaultModelAttribute>) => void

  datasetModels?: Array<DatasetModel>
}
/**
 */
export class DatasetCreateContainer extends React.Component<DatasetCreateProps, any> {
  handleNextStepPickModelForm = () => {
    this.setNextStep()
  }
  handleNextStepPickDatasourceForm = () => {
    this.setNextStep()
  }
  handleNextStepSuccess = () => {
    const urlTo = "/admin/" + this.props.params.projectName + "/datamanagement/"
    browserHistory.push(urlTo)
    this.resetStepAndData()
  }

  resetStepAndData = () => {
    this.props.setViewState(STATES.SELECT_MODELE)
  }
  setNextStep = () => {
    switch (this.props.viewState) {
      case STATES.SELECT_MODELE:
        this.props.setViewState(STATES.SELECT_SOURCE)
        break
      case STATES.SELECT_SOURCE:
        this.props.setViewState(STATES.DONE)
        break
      case STATES.DONE:
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

  goToNewModel = () => {
    const from = encodeURIComponent(this.props.location.pathname)
    const urlTo = "/admin/" + this.props.params.project + "/datamanagement/datasetmodel/create/" + from
    browserHistory.push(urlTo)
  }
  goToNewDatasource = () => {
    const from = encodeURIComponent(this.props.location.pathname)
    const urlTo = "/admin/" + this.props.params.project + "/datamanagement/datasource/create/" + from
    browserHistory.push(urlTo)
  }

  savePickModelForm = (label: string, modelType: number, attributesDefined: Array<DatasetDefaultModelAttribute>) => {
    this.props.setDatasetLabel(label)
    this.props.setDatasetModelType(modelType)
    this.props.setDatasetDefaultModelAttributes(attributesDefined)
  }
  handleGetBack = (state: string) => {
    switch (state) {
      case STATES.SELECT_MODELE:
        this.resetStepAndData()
        const urlTo = "/admin/" + this.props.params.project + "/datamanagement/dataset/"
        browserHistory.push(urlTo)
        break
      case STATES.SELECT_SOURCE:
        this.props.setViewState(STATES.SELECT_MODELE)
        break
      case STATES.DONE:
        this.props.setViewState(STATES.SELECT_SOURCE)
        break
      default:
        throw 'Undefined state ' + state
    }
  }
  handleGetBackToPickModel = () => {
    this.props.setViewState(STATES.DONE)
  }

  render (): JSX.Element {
    const {viewState, datasetModels} = this.props
    const stepper = (
      <StepperCreateDatasetComponent
        getStepperIndex={this.getStepperIndex}
      />
    )
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          {(() => {
            switch (viewState) {
              case STATES.SELECT_MODELE:
                return <PickModelFormComponent
                  handleNextStep={this.handleNextStepPickModelForm}
                  goToNewModel={this.goToNewModel}
                  save={this.savePickModelForm}
                  handleGetBack={() => {this.handleGetBack(STATES.SELECT_MODELE)}}
                  datasetModels={datasetModels}
                >
                  {stepper}
                </PickModelFormComponent>
              case STATES.SELECT_SOURCE:
                return <PickDatasourceFormComponent
                  handleNextStep={this.handleNextStepPickDatasourceForm}
                  handleGetBack={() => {this.handleGetBack(STATES.SELECT_SOURCE)}}
                  goToNewDatasource={this.goToNewDatasource}
                >
                  {stepper}
                </PickDatasourceFormComponent>
              case STATES.DONE:
                return <CreateDatasetSuccessComponent
                  handleNextStep={this.handleNextStepSuccess}
                >
                  {stepper}
                </CreateDatasetSuccessComponent>
              default:
                throw 'Undefined state ' + viewState
            }
          })()}
        </div>
      </I18nProvider>
    )
  }
}

const mapStateToProps = (state: any, ownProps: any) => {
  const viewState = Selectors.getFormDatasetViewState(state)
  const datasetModels = Selectors.getDatasetModels(state)
  return {
    viewState,
    datasetModels
  }
}
const mapDispatchToProps = (dispatch: any) => ({
  setViewState: (newState: string) => dispatch(Actions.setViewState(newState)),
  setDatasetLabel: (label: string) => dispatch(Actions.setDatasetLabel(label)),
  setDatasetModelType: (modelType: number) => dispatch(Actions.setDatasetModelType(modelType)),
  setDatasetDefaultModelAttributes: (attributesDefined: Array<DatasetDefaultModelAttribute>) =>
    dispatch(Actions.setDatasetDefaultModelAttributes(attributesDefined))

})
export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
