import { PROJECT_ACCOUNT_REQUEST, PROJECT_ACCOUNT_SUCCESS, PROJECT_ACCOUNT_FAILURE } from "./actions"

import { ApiStateResult } from '../../../common/models/api/types'
import { Role } from '../../../common/models/users/types'

export default (state:ApiStateResult<Role> = {
  isFetching: false,
  items: [],
  ids: [],
  lastUpdate: ''
}, action: any) => {
  switch (action.type) {
    case PROJECT_ACCOUNT_REQUEST:
      return Object.assign({}, state, {isFetching: true})
    case PROJECT_ACCOUNT_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload.entities.roles,
        ids: []
      })
    case PROJECT_ACCOUNT_FAILURE:
      return Object.assign({}, state, {isFetching: false})
    default:
      return state
  }
}

// export const getRoles = (state: any) => state
export const getRolesById = (state: any, id: string) => state.roles[id]
