import {
  SELECT_PROJECT,
  SELECT_PROJECT_ADMIN,
  SHOW_PROJECT_CONFIGURATION,
  HIDE_PROJECT_CONFIGURATION,
  SHOW_PROJECT_ADMIN_CONFIGURATION,
  HIDE_PROJECT_ADMIN_CONFIGURATION,
} from './actions'

export default (state:Object = {}, action:any) => {
  switch (action.type) {
    case SELECT_PROJECT:
      return Object.assign({}, state,{selectedProjectId: action.id })
    case SELECT_PROJECT_ADMIN:
      return Object.assign({}, state,{selectedProjectAdminId: action.id })
    case SHOW_PROJECT_CONFIGURATION:
      return Object.assign({}, state,{
        projectConfigurationIsShown: true,
        projectAdminConfigurationIsShown: false
      })
    case HIDE_PROJECT_CONFIGURATION:
      return Object.assign({}, state,{projectConfigurationIsShown: false })
    case SHOW_PROJECT_ADMIN_CONFIGURATION:
      return Object.assign({}, state,{
        projectConfigurationIsShown: false,
        projectAdminConfigurationIsShown: true
      })
    case HIDE_PROJECT_ADMIN_CONFIGURATION:
      return Object.assign({}, state,{projectAdminConfigurationIsShown: false })
    default:
      return state
  }
}

// Selectors
export const getSelectedProjectId = (state: any) => state.selectedProjectId
export const getSelectedProjectAdminId = (state: any) => state.selectedProjectAdminId
