import { SELECT_PROJECT, SELECT_PROJECT_ADMIN } from "./actions";

export default (state: Object = {}, action: any) => {
  switch (action.type) {
    case SELECT_PROJECT:
      return Object.assign ({}, state, {selectedProjectId: action.id})
    case SELECT_PROJECT_ADMIN:
      return Object.assign ({}, state, {selectedProjectAdminId: action.id})
    default:
      return state
  }
}

// Selectors
export const getSelectedProjectId = (state: any) => state.selectedProjectId
export const getSelectedProjectAdminId = (state: any) => state.selectedProjectAdminId
