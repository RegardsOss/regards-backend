import { union } from "lodash"
import { deleteEntityReducer } from "../../../common/reducers"
import { PROJECTS_REQUEST, PROJECTS_SUCCESS, PROJECTS_FAILURE, ADD_PROJECT, DELETE_PROJECT, CREATE_PROJECT_SUCCESS } from "./actions"

export default (state: any = {
  isFetching: false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  let newState = Object.assign({}, state)
  switch (action.type) {
    case PROJECTS_REQUEST:
      newState.isFetching = true
      return newState
    case PROJECTS_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload.entities.projects,
        ids: union(state.ids, action.payload.result)
      })
    case PROJECTS_FAILURE:
      return Object.assign({}, state, {isFetching: false})
    case ADD_PROJECT:
      newState.items[action.id] = {
        name: action.name,
        links: []
      }
      newState.ids.push(action.id)
      return newState
    case CREATE_PROJECT_SUCCESS:
      const id = action.payload.result[0]
      const project = action.payload.entities.projects[id]
      newState.items[id] = project
      return newState
    case DELETE_PROJECT:
      return deleteEntityReducer(state, action)
    default:
      return state
  }
}

// Selectors
// WIP
// export const getById = (state: any, id: string) =>
//   state.items[id]
export const getProjects = (state: any) => state
export const getProjectById = (state: any, id: string) => state.items[id]
