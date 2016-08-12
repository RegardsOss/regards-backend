const {CALL_API, getJSON} = require('redux-api-middleware')
import Schemas from "../../../common/api/schemas/index"
import { normalize } from "normalizr"
export const PROJECT_USER_REQUEST = 'ADMIN_PROJECT_USER_REQUEST'
export const PROJECT_USER_SUCCESS = 'ADMIN_PROJECT_USER_SUCCESS'
export const PROJECT_USER_FAILURE = 'ADMIN_PROJECT_USER_FAILURE'

export const PROJECT_USER_DELETE = 'ADMIN_PROJECT_USER_DELETE'

/**
 * Asynchrone Action creator to fetch project user list
 * Fetches all project users
 * Normalize the json response
 *
 * @return {AsyncAction}
 */
const fetchProjectUsers = (urlProjectUsers: String) => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_USER_REQUEST,
      {
        type: PROJECT_USER_SUCCESS,
        payload: (action: any, state: any, res: any) => getJSON(res).then((json: any) => normalize(json, Schemas.USER_ARRAY))
      },
      PROJECT_USER_FAILURE
    ],
    endpoint: urlProjectUsers,
    method: 'GET'
  }
})



/**
 * Action creator to delete project administror by id
 *
 * @param {String} link to the backend endpoint to remove an user
 *
 * @return {AsyncAction}
 */
export function deleteProjectAdmin(link: string): any {
  return {
    type: PROJECT_USER_DELETE,
    link
  }
}

export default {
  fetchProjectUsers,
  deleteProjectAdmin
}
