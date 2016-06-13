import { REQUEST_START_TIME,  RECEIVE_START_TIME }
  from '../actions/TimeActions'
import { SET_TIME } from '../actions/WSTimeActions'

const ws = (state = {
  started: false,
  time: ''
}, action) => {
  switch(action.type){
    case RECEIVE_START_TIME:
      return Object.assign({}, state, {
        started: true
      });
    case SET_TIME:
      return Object.assign({}, state, {
        time: action.time
      });
    default:
      return state;
  }
}

const timeReducers = {
  ws
}

export default timeReducers;
