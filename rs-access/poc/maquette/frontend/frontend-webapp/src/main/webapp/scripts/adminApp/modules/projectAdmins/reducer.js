import { union, values, merge, omitBy, uniqWith, isEqual } from 'lodash'
import { deleteEntityReducer } from 'common/reducers'
import {
  PROJECT_ADMIN_REQUEST,
  PROJECT_ADMIN_SUCESS,
  PROJECT_ADMIN_FAILURE,
  UPDATE_PROJECT_ADMIN,
  UPDATE_OR_CREATE_PROJECT_ADMIN,
  DELETE_PROJECT_ADMIN } from './actions'

export default (state = {
  isFetching : false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action) => {
  switch (action.type) {
    case PROJECT_ADMIN_REQUEST:
      return { ...state, isFetching: true }
    case PROJECT_ADMIN_SUCESS:
      return { ...state,
        isFetching: false,
        items: action.payload.entities.projectAdmins, // TODO: merge with previous items ?
        ids: union(state.ids, action.payload.result)
    }
    case PROJECT_ADMIN_FAILURE:
      return { ...state, isFetching: false }
    case UPDATE_OR_CREATE_PROJECT_ADMIN:
      let newState = state.concat() // Make a shallow copy
      let selectedProjectAdmin = newState.find(pa => pa.id === action.id)
      let projectList = []
      if(selectedProjectAdmin) {
        newState = newState.filter(pa => pa.id !== action.id)
        projectList = selectedProjectAdmin.projects
      }
      newState = newState.concat({
        id: action.id,
        name: action.name,
        projects: uniqWith(projectList.concat(action.projects), isEqual)
        // projects: arrayUnique(projectList.concat(action.projects))
      })
      return newState
    case DELETE_PROJECT_ADMIN:
      return deleteEntityReducer(state, action)
    default:
      return state
  }
}

// Selectors
export const getProjectAdminById = (state, id) => state.items.id

export const getProjectAdminsByProject = (state, project) => values(state.items) // TODO
  // (!project) ? [] : state.items.filter(pa => pa.projects.includes(project.id))
