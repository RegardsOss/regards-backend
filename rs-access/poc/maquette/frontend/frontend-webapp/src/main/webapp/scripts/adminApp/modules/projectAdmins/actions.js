import fetch from 'isomorphic-fetch'

/*********************************************************
***************** Fetch related actions ******************
*********************************************************/

// Backend api adress
export const PROJECT_ADMINS_API='http://localhost:8080/api/project-admins'

// Action to inform store that the project admins request is running
export const REQUEST_PROJECTS_ADMINS = 'REQUEST_PROJECTS_ADMINS'
export function requestProjectAdmins() {
  return {
    type: REQUEST_PROJECTS_ADMINS,
  }
}

// Action to inform store that the project admins are availables
export const RECEIVE_PROJECT_ADMINS = 'RECEIVE_PROJECT_ADMINS'
export function receiveProjectAdmins(projectAdmins) {
  return {
    type: RECEIVE_PROJECT_ADMINS,
    projectAdmins: projectAdmins,
    receivedAt: Date.now()
  }
}

// Action to inform store that the running project admins request failed
export const FAILED_PROJECT_ADMINS = 'FAILED_PROJECT_ADMINS'
function failedProjectAdmins(error) {
  return {
    type : FAILED_PROJECT_ADMINS,
    error : error
  }
}

// Function to check the projects request response status
function checkStatus(response) {
  if (response.status === 200 ) {
    return response
  } else {
    var error = new Error(response.statusText)
    throw error
  }
}

// Asynchrone action to fetch project admins from backend
export function fetchProjectAdmins() {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.
  return function (dispatch, getState) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.
    dispatch(requestProjectAdmins())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    // Init the authorization bearer of the fetch request
    let authentication = getState().common.authentication
    let authorization = "Basic"

    if ( authentication && authentication.user && authentication.user.access_token){
      authorization = "Bearer " + authentication.user.access_token
    }

    return fetch(PROJECT_ADMINS_API, {
      headers: {
        'Accept': 'application/json',
        'Authorization': authorization
      }
    })
    .then(checkStatus)
    .then(function(response) {
      return response.json()
    }).then(function(body) {
      dispatch(receiveProjectAdmins(body))
    }).catch(function(error) {
      dispatch(failedProjectAdmins(error.message))
    })
  }
}

/*********************************************************
******************** Regular actions *********************
*********************************************************/

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
