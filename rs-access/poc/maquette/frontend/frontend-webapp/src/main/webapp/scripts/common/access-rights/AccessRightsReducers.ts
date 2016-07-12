import {
  REQUEST_ACCESSRIGHTS,  RECEIVE_ACCESSRIGHTS,
  FAILED_ACCESSRIGHTS } from './AccessRightsActions'

export default (state:Array<any> = [], action: any) => {
  switch(action.type){
    case REQUEST_ACCESSRIGHTS:
      return state;
    case RECEIVE_ACCESSRIGHTS:
    case FAILED_ACCESSRIGHTS:
    return [...state,{
      name : action.view,
      access: action.access
    }];
    default:
      return state;
  }
}
//
// const AccessRightsReducers = {
//   views
// }
//
// export default AccessRightsReducers
