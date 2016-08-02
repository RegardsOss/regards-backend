/** @module AdminProjectAdmins */
const {CALL_API, getJSON} = require ('redux-api-middleware')
import Schemas from "../../../common/api/schemas";
import { normalize } from "normalizr";
import { ProjectAdmin } from "./types";

export const PROJECT_ADMINS_API = 'http://localhost:8080/api/project-admins'
export const PROJECT_ADMIN_REQUEST = 'PROJECT_ADMIN_REQUEST'
export const PROJECT_ADMIN_SUCESS = 'PROJECT_ADMIN_SUCESS'
export const PROJECT_ADMIN_FAILURE = 'PROJECT_ADMIN_FAILURE'


/**
 * Asynchrone Action creator to fetch project administrors
 * Fetches all project admins
 * Relies on the custom API middleware defined in redux-api-middleware
 * Normalize the json response
 *
 * @return {AsyncAction}
 */
export const fetchProjectAdmins = () => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_ADMIN_REQUEST,
      {
        type: PROJECT_ADMIN_SUCESS,
        payload: (action: any, state: any, res: any) => getJSON (res).then ((json: any) => normalize (json, Schemas.PROJECT_ADMIN_ARRAY))
      },
      PROJECT_ADMIN_FAILURE
    ],
    endpoint: PROJECT_ADMINS_API,
    method: 'GET'
  }
})

/**
 * Asynchrone Action creator to fetch project administror by id
 * Fetches a given administrator user by id
 * Relies on the custom API middleware defined in redux-api-middleware
 * Normalize the json response
 *
 * @param {String} endpoint backend endpoint to fetch project administrator
 *
 * @return {AsyncAction}
 */
export const fetchProjectAdminsBy = (endpoint: string) => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_ADMIN_REQUEST,
      {
        type: PROJECT_ADMIN_SUCESS,
        payload: (action: any, state: any, res: any) => getJSON (res).then ((json: any) => normalize (json, Schemas.PROJECT_ADMIN_ARRAY))
      },
      PROJECT_ADMIN_FAILURE
    ],
    endpoint: endpoint,
    method: 'GET'
  }
})


export const DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN'
/**
 * Action creator to delete project administror by id
 *
 * @param {String} id backend endpoint to fetch project administrator
 *
 * @return {AsyncAction}
 */
export function deleteProjectAdmin(id: string): any {
  return {
    type: DELETE_PROJECT_ADMIN,
    id
  }
}

export const UPDATE_PROJECT_ADMIN = 'UPDATE_PROJECT_ADMIN'
/**
 * Action creator to update project administror
 *
 * @param {String} id User identifier
 * @param {Object} payload User to update
 *
 * @return {AsyncAction}
 */
export function updateProjectAdmin(id: string, payload: any): any {
  return {
    type: UPDATE_PROJECT_ADMIN,
    id,
    payload
  }
}

export const CREATE_PROJECT_ADMIN = 'CREATE_PROJECT_ADMIN'
/**
 * Create a project admin
 * @param  {any}
 * @param  {ProjectAdmin}
 * @return {CreateProjectAdminAction}
 */
export function createProjectAdmin(id: any, payload: any): any {
  return {
    type: CREATE_PROJECT_ADMIN,
    id,
    payload
  }
}

/**
 * Update or create a project admin
 * If the user already exists, it will be updated,
 * else it will be created
 *
 * @param {String} id of the project admin to update/create
 * @param {ProjectAdmin} the project admin's payload
 */
export const UPDATE_OR_CREATE_PROJECT_ADMIN = 'UPDATE_OR_CREATE_PROJECT_ADMIN'
export const updateOrCreateProjectAdmin = (id: string, payload: ProjectAdmin): any => ({
  type: UPDATE_OR_CREATE_PROJECT_ADMIN,
  id,
  payload
})
