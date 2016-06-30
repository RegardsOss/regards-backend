import { CALL_API, getJSON } from 'redux-api-middleware'
import Schemas from 'common/api/schemas'
import { getAuthorization } from 'common/reducers'
import { normalize } from 'normalizr'

const PROJECT_ADMINS_API='http://localhost:8080/api/project-admins'
export const PROJECT_ADMIN_REQUEST = 'PROJECT_ADMIN_REQUEST'
export const PROJECT_ADMIN_SUCESS  = 'PROJECT_ADMIN_SUCESS'
export const PROJECT_ADMIN_FAILURE = 'PROJECT_ADMIN_FAILURE'

// Fetches all project admins
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
export const fetchProjectAdmins = () => ({
// export const fetchProjectAdmins = (key, dataObject) => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_ADMIN_REQUEST,
      {
        type: PROJECT_ADMIN_SUCESS,
        payload: (action, state, res) => getJSON(res).then((json) => normalize(json, Schemas.PROJECT_ADMIN_ARRAY))
      },
      PROJECT_ADMIN_FAILURE
    ],
    endpoint: PROJECT_ADMINS_API,
    method: 'GET'
  }
})

// Delete a project admins from the list
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
