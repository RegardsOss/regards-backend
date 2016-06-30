import { union, values, merge, omitBy } from 'lodash'
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
        projects: arrayUnique(projectList.concat(action.projects))
      })
      return newState
    case DELETE_PROJECT_ADMIN:
      return { ... state,
        items: omitBy(state.items, (value, key) => key !== action.id),
        ids: state.ids.filter(id => id !== action.id)
      }
    default:
      return state
  }
}

const arrayUnique = (array) => {
    var a = array.concat();
    for(var i=0; i<a.length; ++i) {
        for(var j=i+1; j<a.length; ++j) {
            if(a[i] === a[j])
                a.splice(j--, 1);
        }
    }
    return a;
}

// Selectors
export const getProjectAdminById = (state, id) => state.items.id

export const getProjectAdminsByProject = (state, project) => values(state.items) // TODO
  // (!project) ? [] : state.items.filter(pa => pa.projects.includes(project.id))
