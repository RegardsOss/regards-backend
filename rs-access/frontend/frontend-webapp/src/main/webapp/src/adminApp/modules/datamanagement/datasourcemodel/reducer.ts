import { ADD_DATASOURCE_MODEL } from "./actions"

const predefinedValues: any = {
  1: {
    name: "Modèle datasource exemple CDPP",
    id: 1,
    attributes: [{
      name: "SIZE",
      type: "integer"
    }, {
      name: "START_DATE",
      type: "integer"
    }, {
      name: "STOP_DATE",
      type: "integer"
    }, {
      name: "MIN_LONGITUDE",
      type: "integer"
    }, {
      name: "MAX_LONGITUDE",
      type: "integer"
    }, {
      name: "MIN_LATITUDE",
      type: "string"
    }, {
      name: "MIN_ALTITUDE",
      type: "string"
    }, {
      name: "MAX_ALTITUDE",
      type: "string"
    }, {
      name: "PROCESSING_LEVEL",
      type: "string"
    }, {
      name: "QUALITY",
      type: "string"
    }, {
      name: "OPERATIONAL_REQUIREMENTS",
      type: "string"
    }, {
      name: "ACCESS_REQUIREMENTS",
      type: "string"
    }, {
      name: "FILE_FORMAT",
      type: "string"
    }, {
      name: "INSTRUMENT_TYPE",
      type: "string"
    }, {
      name: "MEASUREMENT_TYPE",
      type: "string"
    }]
  }
}

export default (state: any = {
  isFetching: false,
  items: predefinedValues, // TODO -> should be empty here
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    case ADD_DATASOURCE_MODEL:
      let newState = Object.assign({}, state)
      newState.items[action.entity.id] = action.entity
      return newState
    default:
      return state
  }
}

export const getDatasourceModel = (state: any) => state.items
export const getDatasourceModelById = (state: any, id: number) => state.items[id]
