import {
  SET_DATASET_DEFAULTMODELATTRIBUTES,
  SET_DATASET_MODELTYPE,
  SET_DATASET_LABEL,
  SET_DATASOURCE,
  SET_VIEW_STATE
} from "./formActions"
import { STATES as ViewStates } from "./containers/DatasetCreateContainer"

export default (state: any = {
  datasourceId: undefined,
  label: "",
  modelType: "",
  defaultModelAttributes: [], // TODO à changer ;-)
  viewState: ViewStates.SELECT_MODELE,
}, action: any) => {
  switch (action.type) {
    case SET_DATASOURCE:
      return Object.assign({}, state, {idDatasource: action.idDatasource})
    case SET_VIEW_STATE:
      return Object.assign({}, state, {viewState: action.viewState})
    case SET_DATASET_DEFAULTMODELATTRIBUTES:
      return Object.assign({}, state, {defaultModelAttributes: action.defaultModelAttributes})
    case SET_DATASET_MODELTYPE:
      return Object.assign({}, state, {modelType: action.modelType})
    case SET_DATASET_LABEL:
      return Object.assign({}, state, {label: action.label})
    default:
      return state
  }
}


// Selectors
export const getFormDatasetViewState = (state: any) => state.viewState
