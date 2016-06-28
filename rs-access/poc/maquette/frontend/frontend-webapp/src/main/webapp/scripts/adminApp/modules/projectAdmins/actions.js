// Delete a project from the list
export const DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN'
export function deleteProjectAdmin(id) {
  return {
    type: DELETE_PROJECT_ADMIN,
    id
  }
}

export const UPDATE_PROJECT_ADMIN = 'UPDATE_PROJECT_ADMIN'
export function updateProjectAdmin(projectAdmin) {
  return {
    type: UPDATE_PROJECT_ADMIN,
    projectAdmin
  }
}

/**
 * [UPDATE_OR_CREATE_PROJECT_ADMIN description]
 * @type {String} id of the project admin to update/create
 * @type {String} name of the project admin to update/create
 * @type {String} list to projects ids to associate the project admin to
 */
export const UPDATE_OR_CREATE_PROJECT_ADMIN = 'UPDATE_OR_CREATE_PROJECT_ADMIN'
export function updateOrCreateProjectAdmin(id, name, projects) {
  return {
    type: UPDATE_OR_CREATE_PROJECT_ADMIN,
    id,
    name,
    projects
  }
}
