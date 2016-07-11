import {
  REQUEST_PROJECTS,  RECEIVE_PROJECTS,
  FAILED_PROJECTS } from '../actions/ProjectsActions'

import { ProjectsStore } from '../components/ProjectTypes'

export default (state:ProjectsStore = {
  isFetching : false,
  items: [],
  lastUpdate: ''
}, action:any) => {
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
