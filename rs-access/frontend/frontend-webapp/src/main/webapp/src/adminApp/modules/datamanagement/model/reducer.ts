import { ADD_MODEL } from "./actions"


const predefinedValues: any = {
  1: {
    name: 'AIR_SPACIAL_COMPLEX',
    id: 1,
    attributes: [{
      label: "Attr1",
      type: "integer"
    }, {
      label: "Attr1",
      type: "float"
    }, {
      label: "Attr1",
      type: "string"
    }, {
      label: "Attr1",
      type: "geometric"
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
export const getModel = (state: any) => state
export const getModelById = (state: any, id: string) => state.items[id]
