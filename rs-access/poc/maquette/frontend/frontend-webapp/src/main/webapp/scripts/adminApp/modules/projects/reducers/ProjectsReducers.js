import { REQUEST_PROJECTS,RECEIVE_PROJECTS, FAILED_PROJECTS } from '../actions/ProjectsActions'
import {
  SELECT_PROJECT, DELETE_PROJECT,
  SHOW_PROJECT_CONFIGURATION, HIDE_PROJECT_CONFIGURATION,
  SHOW_ADMIN_CONFIGURATION, HIDE_ADMIN_CONFIGURATION } from '../actions/ProjectsActions'

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
      return callIhmReducers(state, action)
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

const callIhmReducers = (state, action) => {
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
    case DELETE_PROJECT:
      console.log('coucou');
      return Object.assign({}, state, {
        items: state.items.filter(project => project.id !== action.id)
      })
    case SHOW_PROJECT_CONFIGURATION:
      nextState.projectConfigurationIsShown = true;
      nextState.adminConfigurationIsShown = false;
      return nextState
    case HIDE_PROJECT_CONFIGURATION:
      nextState.projectConfigurationIsShown = false;
      return nextState
    case SHOW_ADMIN_CONFIGURATION:
      nextState.adminConfigurationIsShown = true;
      nextState.projectConfigurationIsShown = false;
      return nextState
    case HIDE_ADMIN_CONFIGURATION:
      nextState.adminConfigurationIsShown = false;
      return nextState
    default:
      return state
  }
}
