/** @module AdminProjectAdmins */
import { union, values, merge, isEqual, uniq } from 'lodash'
import { deleteEntityReducer } from '../../../common/reducers'
import {
  PROJECT_ADMIN_REQUEST,
  PROJECT_ADMIN_SUCESS,
  PROJECT_ADMIN_FAILURE,
  CREATE_PROJECT_ADMIN,
  UPDATE_PROJECT_ADMIN,
  UPDATE_OR_CREATE_PROJECT_ADMIN,
  DELETE_PROJECT_ADMIN } from './actions'

/**
 * ProjectAdmins reducer.
 *
 * @prop {Object} state
 * @prop {Action} action Action to reduce type can be [PROJECT_ADMIN_REQUEST|PROJECT_ADMIN_SUCESS|PROJECT_ADMIN_FAILUREUPDATE_OR_CREATE_PROJECT_ADMIN|
 * UPDATE_PROJECT_ADMIN|CREATE_PROJECT_ADMIN|DELETE_PROJECT_ADMIN]
 */
export const projectAdminsReducer =  (state: any = {
  isFetching : false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  let newState = Object.assign({}, state)
  switch (action.type) {
    case PROJECT_ADMIN_REQUEST:
      newState.isFetching = true
      return newState
    case PROJECT_ADMIN_SUCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload.entities.projectAdmins, // TODO: merge with previous items ?
        ids: union(state.ids, action.payload.result)
      })
    case PROJECT_ADMIN_FAILURE:
      return Object.assign({}, state, { isFetching: false })
    case UPDATE_OR_CREATE_PROJECT_ADMIN:
      newState.items[action.id] = action.payload
      newState.ids.push(action.id)
      newState.ids = uniq(newState.ids)
      return newState
    case UPDATE_PROJECT_ADMIN:
      newState.items[action.id] = action.payload
      return newState
    case CREATE_PROJECT_ADMIN:
      newState.items[action.id] = action.payload
      newState.ids.push(action.id)
      return newState
    case DELETE_PROJECT_ADMIN:
      return deleteEntityReducer(state, action)
    default:
      return state
  }
}

// Selectors
// WIP
// export const getById = (state: any, id: string) =>
//   state.items[id]

export const getProjectAdminById = (state: any, id: string) => state.items[id]
export const getProjectAdminsByProject = (state: any, project: string) => values(state.items) // TODO
  // (!project) ? [] : state.items.filter(pa => pa.projects.includes(project.id))
export default projectAdminsReducer
