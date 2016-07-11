import { RECEIVE_START_TIME }
  from '../actions/TimeActions'
import { SET_TIME } from '../actions/WSTimeActions'

export default (state:any = {
  started: false,
  time: ''
}, action:any) => {
  switch(action.type){
    case RECEIVE_START_TIME:
      return Object.assign({}, state, {
        started: true
      })
    case SET_TIME:
      return Object.assign({}, state, {
        time: action.time
      })
    default:
      return state
  }
}

// const timeReducers = {
//   ws
// }
//
// export default timeReducers
