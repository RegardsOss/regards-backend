import { union, values, merge, omitBy, uniqWith, isEqual } from 'lodash'
import { deleteEntityReducer } from 'common/reducers'
import {
  PROJECTS_REQUEST,
  PROJECTS_SUCESS,
  PROJECTS_FAILURE,
  SELECT_PROJECT,
  ADD_PROJECT,
  DELETE_PROJECT,
  DELETE_PROJECT_ADMIN } from './actions'

export default (state = {
  isFetching : false,
  items: [],
  ids: [],
  lastUpdate: ''
}, action) => {
  switch(action.type){
    case PROJECTS_REQUEST:
      return { ...state, isFetching: true }
    case PROJECTS_SUCESS:
      return { ...state,
        isFetching: false,
        items: action.payload.entities.projects, // TODO: merge with previous items ?
        ids: union(state.ids, action.payload.result)
    }
    case PROJECTS_FAILURE:
      return { ...state, isFetching: false }
    case ADD_PROJECT:
      return { ...state,
        items: state.items.concat({
          id: action.id,
          name: action.name,
          selected: false,
          admins: []
        })
      }
    case DELETE_PROJECT:
      return deleteEntityReducer(state, action)
    default:
      return state
  }
}

// Selectors
export const getProjects = (state) => state

export const getProjectById = (state, id) => state.items[id]
