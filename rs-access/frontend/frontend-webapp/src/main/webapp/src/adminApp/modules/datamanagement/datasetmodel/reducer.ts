import { ADD_DATASET_MODEL } from "./actions"

const predefinedValues: any = {
  1: {
    name: "Modèle dataset #1",
    id: 1,
    attributes: [{
      name: "SIZE",
      computed: false,
      type: "integer"
    }, {
      name: "START_DATE",
      computed: false,
      type: "integer"
    }, {
      name: "STOP_DATE",
      computed: false,
      type: "integer"
    }, {
      name: "MIN_LONGITUDE",
      computed: false,
      type: "integer"
    }, {
      name: "MAX_LONGITUDE",
      computed: false,
      type: "integer"
    }, {
      name: "MIN_LATITUDE",
      computed: false,
      type: "string"
    }, {
      name: "MIN_ALTITUDE",
      computed: false,
      type: "string"
    }, {
      name: "MAX_ALTITUDE",
      computed: false,
      type: "string"
    }, {
      name: "PROCESSING_LEVEL",
      computed: false,
      type: "string"
    }, {
      name: "QUALITY",
      computed: false,
      type: "string"
    }, {
      name: "OPERATIONAL_REQUIREMENTS",
      computed: false,
      type: "string"
    }, {
      name: "ACCESS_REQUIREMENTS",
      computed: false,
      type: "string"
    }, {
      name: "FILE_FORMAT",
      computed: false,
      type: "string"
    }, {
      name: "INSTRUMENT_TYPE",
      computed: false,
      type: "string"
    }, {
      name: "MEASUREMENT_TYPE",
      computed: false,
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
    case ADD_DATASET_MODEL:
      let newState = Object.assign({}, state)
      newState.items[action.entity.id] = action.entity
      return newState
    default:
      return state
  }
}

export const getDatasetModel = (state: any) => state.items
export const getDatasetModelById = (state: any, id: number) => state.items[id]
