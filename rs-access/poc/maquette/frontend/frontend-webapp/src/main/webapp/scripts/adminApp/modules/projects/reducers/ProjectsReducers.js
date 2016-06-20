import { REQUEST_PROJECTS,RECEIVE_PROJECTS, FAILED_PROJECTS } from '../actions/ProjectsActions'
import { SELECT_PROJECT } from '../actions/ProjectsActions'

const projects = (state = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action) => {
  switch(action.type){
    case REQUEST_PROJECTS:
    case RECEIVE_PROJECTS:
    case FAILED_PROJECTS:
      return callFetchReducers(state, action)
    case SELECT_PROJECT:
      return callIhmReducers(state, action)
    default:
      return state
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
      break;
    default:
      return state
  }
}

const projectsReducers = {
  projects
}

export default projectsReducers
