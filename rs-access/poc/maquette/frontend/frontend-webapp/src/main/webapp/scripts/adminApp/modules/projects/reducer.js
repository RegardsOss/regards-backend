import {
  REQUEST_PROJECTS,
  RECEIVE_PROJECTS,
  FAILED_PROJECTS,
  SELECT_PROJECT,
  ADD_PROJECT,
  DELETE_PROJECT,
  DELETE_PROJECT_ADMIN } from './actions'

export default (state = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action) => {
  switch(action.type){
    case REQUEST_PROJECTS:
    case RECEIVE_PROJECTS:
    case FAILED_PROJECTS:
      return callFetchReducers(state, action)
    default:
      return callProjectsReducers(state, action)
    }
};

const callFetchReducers = (state = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action) => {
  switch(action.type){
    case REQUEST_PROJECTS:
      return Object.assign({}, state, {
        isFetching: true
      })
    case RECEIVE_PROJECTS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.projects,
        lastUpdate: action.receivedAt
      })
    case FAILED_PROJECTS:
      return Object.assign({}, state, {
        isFetching: false
      })
    default:
      return state
  }
}

const callProjectsReducers = (state, action) => {
  let nextState = Object.assign({}, state)
  switch (action.type) {
    case SELECT_PROJECT:
      // Deselect previously selected project
      let oldSelected = nextState.items.find(project => project.selected)
      if(oldSelected) oldSelected.selected = false
      // Selected right one
      let newSelected = nextState.items.find(project => project.id === action.id)
      if(newSelected) newSelected.selected = true
      return nextState
    case ADD_PROJECT:
      nextState.items = nextState.items.concat({
        id: action.id,
        name: action.name,
        selected: false,
        admins: []
      })
      return nextState
    case DELETE_PROJECT:
      return Object.assign({}, state, {
        items: state.items.filter(project => project.id !== action.id)
      })
    case DELETE_PROJECT_ADMIN:
      // throw new Error('Not implemented yet!!')
      return nextState
    default:
      return state
  }
}

// Selectors
export const getProjects = (state) => state.items

export const getProjectById = (state, id) =>
  state.items.find(p => p.id === id)
