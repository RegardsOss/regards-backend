import { PROJECT_ACCOUNT_REQUEST, PROJECT_ACCOUNT_SUCCESS, PROJECT_ACCOUNT_FAILURE } from "./actions"
import { ApiStateResult, NormalizedAction } from "../../../common/models/api/types"
import { Account } from "../../../common/models/users/types"

export default (state: ApiStateResult<Account> = {
  isFetching: false,
  items: [],
  ids: [],
  lastUpdate: ''
}, action: NormalizedAction) => {
  switch (action.type) {
    case PROJECT_ACCOUNT_REQUEST:
      return Object.assign({}, state, {isFetching: true})
    case PROJECT_ACCOUNT_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload.entities.accounts,
        ids: []
      })
    case PROJECT_ACCOUNT_FAILURE:
      return Object.assign({}, state, {isFetching: false})
    default:
      return state
  }
}

// Selectors
export const getAccountsId = (state: any) => state.items
export const getAccountById = (state: any, id: number) => state.items[id]
