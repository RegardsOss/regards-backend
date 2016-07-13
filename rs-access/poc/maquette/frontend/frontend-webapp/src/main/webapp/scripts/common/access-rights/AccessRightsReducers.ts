import {
  REQUEST_ACCESSRIGHTS,  RECEIVE_ACCESSRIGHTS,
  FAILED_ACCESSRIGHTS } from './AccessRightsActions'

export default (state:any = {
  isFetching: false,
  items: []
}, action: any) => {
  let newState = Object.assign({}, state)
  switch(action.type){
    case REQUEST_ACCESSRIGHTS:
      newState.isFetching = true
      return newState
    case RECEIVE_ACCESSRIGHTS:
      newState.isFetching = false
      newState.items.push({
        name : action.view,
        access: action.access
      })
      return newState
      case FAILED_ACCESSRIGHTS:
        newState.isFetching = false
        return newState
    default:
      return state;
  }
}
