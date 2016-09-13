import { PROJECT_ACCOUNT_REQUEST, PROJECT_ACCOUNT_SUCCESS, PROJECT_ACCOUNT_FAILURE } from "./actions"
import { ApiStateResult, NormalizedAction, ProjectAccount } from "@regardsoss/models"

export default (state: ApiStateResult<ProjectAccount> = {
  isFetching: false,
  items: [],
  ids: [],
  lastUpdate: ''
}, action: NormalizedAction) => {
  switch (action.type) {
    case PROJECT_ACCOUNT_REQUEST:
      return Object.assign({}, state, { isFetching: true })
    case PROJECT_ACCOUNT_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        items: action.payload.entities.projectAccounts,
        ids: []
      })
    case PROJECT_ACCOUNT_FAILURE:
      return Object.assign({}, state, { isFetching: false })
    default:
      return state
  }
}

// Selectors
export const getProjectAccounts = (state: any) => state.items
export const getProjectAccountById = (state: any, id: number) => state.items[id]

// export const getRoles = (state: any) => state
export const getRolesById = (state: any, id: string) => state.roles[id]
