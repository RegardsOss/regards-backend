import { union } from "lodash"
import { deleteEntityReducer } from "../../../common/reducers"
import { PROJECTS_REQUEST, PROJECTS_SUCCESS, PROJECTS_FAILURE, ADD_PROJECT, DELETE_PROJECT } from "./actions"

export default (state: any = {
  isFetching: false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    case PROJECTS_REQUEST:
      return Object.assign ({}, state, {isFetching: true})
    case PROJECTS_SUCCESS:
      return Object.assign ({}, state, {
        isFetching: false,
        items: action.payload.entities.projects,
        ids: union (state.ids, action.payload.result)
      })
    case PROJECTS_FAILURE:
      return Object.assign ({}, state, {isFetching: false})
    case ADD_PROJECT:
      let newState = Object.assign ({}, state)
      newState.items[action.id] = {
        name: action.name,
        links: []
      }
      newState.ids.push (action.id)
      return newState
    case DELETE_PROJECT:
      return deleteEntityReducer (state, action)
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
