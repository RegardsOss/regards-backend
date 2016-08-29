import { ADD_MODEL } from "./actions"
import { Model } from "./Model"

const predefinedValues: any = {
  1: {
    name: 'AIR_SPACIAL_COMPLEX',
    id: 1,
    attributes: [{
      name: "Int #1",
      type: "integer"
    }, {
      name: "Int #2",
      type: "integer"
    }, {
      name: "Some string #1",
      type: "string"
    }, {
      name: "Some string #2",
      type: "string"
    }]
  }
}

export default (state: any = {
  isFetching: false,
  items: predefinedValues, // TODO -> should be empty here
  ids: [1],
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    case ADD_MODEL:
      let newState = Object.assign({}, state)
      newState.items[action.entity.id] = action.entity
      newState.ids.push(action.entity.id)
      return newState
    default:
      return state
  }
}

// Selectors
// WIP
// export const getById = (state: any, id: string) =>
//   state.items[id]
export const getModel = (state: any) => state.items
export const getModelById = (state: any, id: string) => state.items[id]
