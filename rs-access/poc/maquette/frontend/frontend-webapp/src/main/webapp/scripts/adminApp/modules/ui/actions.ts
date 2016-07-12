// Show the project configuration view
export const SHOW_PROJECT_CONFIGURATION = 'SHOW_PROJECT_CONFIGURATION'
export function showProjectConfiguration() {
  return {
    type: SHOW_PROJECT_CONFIGURATION
  }
}

// Hide the project configuration view
export const HIDE_PROJECT_CONFIGURATION = 'HIDE_PROJECT_CONFIGURATION'
export function hideProjectConfiguration() {
  return {
    type: HIDE_PROJECT_CONFIGURATION
  }
}

// Select a project in the IHM
export const SELECT_PROJECT = 'SELECT_PROJECT'
export function selectProject(id: string) {
  return {
    type: SELECT_PROJECT,
    id
  }
}

// Delete a project from the list
export const DELETE_PROJECT = 'DELETE_PROJECT'
export function deleteProject(id: string) {
  return {
    type: DELETE_PROJECT,
    id
  }
}

// Set active a project admin in the IHM
export const SELECT_PROJECT_ADMIN = 'SELECT_PROJECT_ADMIN'
export function selectProjectAdmin(id: string) {
  return {
    type: SELECT_PROJECT_ADMIN,
    id
  }
}

// Delete a project from the list
export const DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN'
export function deleteProjectAdmin(id: string) {
  return {
    type: DELETE_PROJECT_ADMIN,
    id
  }
}

// Show the admin configuration view
export const SHOW_PROJECT_ADMIN_CONFIGURATION = 'SHOW_PROJECT_ADMIN_CONFIGURATION'
export function showProjectAdminConfiguration() {
  return {
    type: SHOW_PROJECT_ADMIN_CONFIGURATION
  }
}

// Hide the admin configuration view
export const HIDE_PROJECT_ADMIN_CONFIGURATION = 'HIDE_PROJECT_ADMIN_CONFIGURATION'
export function hideProjectAdminConfiguration() {
  return {
    type: HIDE_PROJECT_ADMIN_CONFIGURATION
  }
}
