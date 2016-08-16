import { PROJECT_USER_REQUEST, PROJECT_USER_SUCCESS, PROJECT_USER_FAILURE } from "./actions"

export default (state: any = {
  isFetching: false,
  items: {},
  ids: [],
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    case PROJECT_USER_REQUEST:
      return Object.assign({}, state, {isFetching: true})
    case PROJECT_USER_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload,
        ids: []
      })
    case PROJECT_USER_FAILURE:
      return Object.assign({}, state, {isFetching: false})
    default:
      return state
  }
}
