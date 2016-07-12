import {
  REQUEST_AUTHENTICATE,  RECEIVE_AUTHENTICATE,
  FAILED_AUTHENTICATE, LOGOUT } from './AuthenticateActions'

export default (state: any = {
  isFetching : false,
  user: {},
  authenticateDate: '',
  error: ''
}, action: any) => {
  switch(action.type){
    case REQUEST_AUTHENTICATE:
      return Object.assign({}, state, {
        isFetching: true
      });
    case RECEIVE_AUTHENTICATE:
      return Object.assign({}, state, {
        isFetching: false,
        user: action.payload,
        authenticateDate: action.meta.authenticateDate
      });
    case FAILED_AUTHENTICATE:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case LOGOUT:
      return {
        isFetching : false,
        user: {},
        authenticateDate: '',
        error: ''
      }
    default:
      return state;
  }
}
