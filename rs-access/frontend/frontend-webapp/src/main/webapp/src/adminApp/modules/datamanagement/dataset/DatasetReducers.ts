import { SET_DATASET_ATTRIBUTES, SET_DATASOURCE, SET_VIEW_STATE } from "./DatasetActions"
import { STATES as ViewStates } from "./containers/DatasetCreateContainer"

export default (state: any = {
  datasetAttributes: {
    datasourceId: undefined,
    label: "",
    modelAttributes: []
  },
  viewState: ViewStates.SELECT_MODELE,
}, action: any) => {
  switch (action.type) {
    case SET_DATASET_ATTRIBUTES:
      return Object.assign({}, state, {datasetAttributes: {modelAttributes: action.datasetAttributes}})
    case SET_DATASOURCE:
      return Object.assign({}, state, {idDatasource: action.idDatasource})
    case SET_VIEW_STATE:
      return Object.assign({}, state, {viewState: action.viewState})
    default:
      return state
  }
}


// Selectors
export const getFormViewState = (state: any) => state.viewState
export const getFormDatasetAttributes = (state: any) => state.datasetAttributes

