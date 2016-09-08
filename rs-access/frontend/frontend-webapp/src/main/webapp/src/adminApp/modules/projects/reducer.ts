import { union, pickBy } from "lodash"
import { deleteEntityReducer } from "../../../common/reducers"
import {
  PROJECTS_REQUEST, PROJECTS_SUCCESS, PROJECTS_FAILURE,
  DELETE_PROJECT_REQUEST, DELETE_PROJECT_SUCCESS, DELETE_PROJECT_FAILURE,
  CREATE_PROJECT_SUCCESS, CREATE_PROJECT_REQUEST, CREATE_PROJECT_FAILURE
} from "./actions"

export default (state: any = {
  isFetching: false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  let newState = Object.assign({}, state)
  switch (action.type) {
    case PROJECTS_REQUEST:
    case CREATE_PROJECT_REQUEST:
    case DELETE_PROJECT_REQUEST:
      newState.isFetching = true
      return newState
    case PROJECTS_FAILURE:
    case CREATE_PROJECT_FAILURE:
    case DELETE_PROJECT_FAILURE:
      newState.isFetching = false
      return newState
    case PROJECTS_SUCCESS:
      newState.isFetching = false
      newState.items = action.payload.entities.projects
      newState.ids = union(state.ids, action.payload.result)
      return newState
    case CREATE_PROJECT_SUCCESS:
      const project = action.payload.entities.projects[action.payload.result[0]]
      newState.items[action.payload.result[0]] = project
      newState.ids.push(action.payload.result[0])
      newState.isFetching = false
      return newState
    case DELETE_PROJECT_SUCCESS:
      newState.items = pickBy(state.items, (value: string, key: string) => key != action.payload.result[0])
      newState.ids = state.ids.filter((id: string) => id != action.payload.result[0])
      newState.isFetching = false
      return newState
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
