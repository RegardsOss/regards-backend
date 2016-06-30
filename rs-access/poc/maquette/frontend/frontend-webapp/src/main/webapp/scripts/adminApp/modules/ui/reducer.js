import {
  SELECT_PROJECT,
  SELECT_PROJECT_ADMIN,
  SHOW_PROJECT_CONFIGURATION,
  HIDE_PROJECT_CONFIGURATION,
  SHOW_PROJECT_ADMIN_CONFIGURATION,
  HIDE_PROJECT_ADMIN_CONFIGURATION,
} from './actions'

export default (state = [], action) => {
  switch (action.type) {
    case SELECT_PROJECT:
      return { ...state, selectedProjectId: action.id }
    case SELECT_PROJECT_ADMIN:
      return { ...state, selectedProjectAdminId: action.id }
    case SHOW_PROJECT_CONFIGURATION:
      return { ...state,
        projectConfigurationIsShown: true,
        projectAdminConfigurationIsShown: false
      }
    case HIDE_PROJECT_CONFIGURATION:
      return { ...state, projectConfigurationIsShown: false }
    case SHOW_PROJECT_ADMIN_CONFIGURATION:
      return { ...state,
        projectConfigurationIsShown: false,
        projectAdminConfigurationIsShown: true
      }
    case HIDE_PROJECT_ADMIN_CONFIGURATION:
      return { ...state, projectAdminConfigurationIsShown: false }
    default:
      return state
  }
}

// Selectors
export const getSelectedProjectId = (state) => state.selectedProjectId
export const getSelectedProjectAdminId = (state) => state.selectedProjectAdminId
