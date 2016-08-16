const {CALL_API, getJSON} = require('redux-api-middleware')
import Schemas from "../../../common/api/schemas/index"
import { normalize } from "normalizr"

import { User } from "./types"

export const PROJECT_USER_REQUEST = 'PROJECT_USER_REQUEST'
export const PROJECT_USER_SUCCESS = 'PROJECT_USER_SUCCESS'
export const PROJECT_USER_FAILURE = 'PROJECT_USER_FAILURE'

export const PROJECT_USER_DELETE = 'PROJECT_USER_DELETE'

const URL_PROJECTS_USERS = "http://localhost:8080/api/seb/users"

/**
 * Asynchrone Action creator to fetch project user list
 * Fetches all project users
 * Normalize the json response
 *
 * @return {AsyncAction}
 */
export const fetchUsers = () => ({
  [CALL_API]: {
    // endpointKey : key,
    // links: dataObject.links,
    types: [
      PROJECT_USER_REQUEST,
      {
        type: PROJECT_USER_SUCCESS,
        payload: (action: any, state: any, res: any) => getJSON(res).then((json: any) => json)
      },
      PROJECT_USER_FAILURE
    ],
    endpoint: URL_PROJECTS_USERS,
    method: 'GET'
  }
})

export const deleteUser = (user:User) => {
  console.log("NOT IMPLEMENTED")
}
