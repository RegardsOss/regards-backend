// Select a project in the IHM
export const SELECT_PROJECT = 'SELECT_PROJECT'
export function selectProject(id: string): Object {
  return {
    type: SELECT_PROJECT,
    id
  }
}

// Set active a project admin in the IHM
export const SELECT_PROJECT_ADMIN = 'SELECT_PROJECT_ADMIN'
export function selectProjectAdmin(id: string): Object {
  return {
    type: SELECT_PROJECT_ADMIN,
    id
  }
}
