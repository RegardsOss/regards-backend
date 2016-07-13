var { CALL_API, getJSON } = require('redux-api-middleware')
import Schemas from '../../../common/api/schemas'
import { normalize } from 'normalizr'

const PROJECT_ADMINS_API='http://localhost:8080/api/project-admins'
export const PROJECT_ADMIN_REQUEST = 'PROJECT_ADMIN_REQUEST'
export const PROJECT_ADMIN_SUCESS  = 'PROJECT_ADMIN_SUCESS'
export const PROJECT_ADMIN_FAILURE = 'PROJECT_ADMIN_FAILURE'

// Fetches all project admins
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
export const fetchProjectAdmins = () => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_ADMIN_REQUEST,
      {
        type: PROJECT_ADMIN_SUCESS,
        payload: (action: any, state: any, res: any) => getJSON(res).then((json: any) => normalize(json, Schemas.PROJECT_ADMIN_ARRAY))
      },
      PROJECT_ADMIN_FAILURE
    ],
    endpoint: PROJECT_ADMINS_API,
    method: 'GET'
  }
})

// Fetches all project admins
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
export const fetchProjectAdminsBy = (endpoint: any) => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_ADMIN_REQUEST,
      {
        type: PROJECT_ADMIN_SUCESS,
        payload: (action: any, state: any, res: any) => getJSON(res).then((json: any) => normalize(json, Schemas.PROJECT_ADMIN_ARRAY))
      },
      PROJECT_ADMIN_FAILURE
    ],
    endpoint: endpoint,
    method: 'GET'
  }
})

// Delete a project admins from the list
export const DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN'
export function deleteProjectAdmin(id: string) {
  return {
    type: DELETE_PROJECT_ADMIN,
    id
  }
}

export const UPDATE_PROJECT_ADMIN = 'UPDATE_PROJECT_ADMIN'
export function updateProjectAdmin(projectAdmin: any) {
  return {
    type: UPDATE_PROJECT_ADMIN,
    projectAdmin
  }
}

/**
 * [UPDATE_OR_CREATE_PROJECT_ADMIN description]
 * @param {String} id of the project admin to update/create
 * @param {String} name of the project admin to update/create
 * @param {String} list to projects ids to associate the project admin to
 */
export const UPDATE_OR_CREATE_PROJECT_ADMIN = 'UPDATE_OR_CREATE_PROJECT_ADMIN'
export function updateOrCreateProjectAdmin(id: string, name: string, projects: Array<any>) {
  return {
    type: UPDATE_OR_CREATE_PROJECT_ADMIN,
    id,
    name,
    projects
  }
}
