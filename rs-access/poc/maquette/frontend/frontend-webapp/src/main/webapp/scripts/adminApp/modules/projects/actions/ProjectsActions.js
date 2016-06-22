import fetch from 'isomorphic-fetch'

/*********************************************************
***************** Fetch related actions ********************
*********************************************************/

// Backend api adress
export const PROJECTS_API='http://localhost:8080/api/projects'

// Action to inform store that te projects request is running
export const REQUEST_PROJECTS = 'REQUEST_PROJECTS'
export function requestProjects() {
  return {
    type: REQUEST_PROJECTS,
  }
}

// Action to inform store that te projects are availables
export const RECEIVE_PROJECTS = 'RECEIVE_PROJECTS'
export function receiveProjects(projects) {
  return {
    type: RECEIVE_PROJECTS,
    projects: projects,
    receivedAt: Date.now()
  }
}

// Action to inform store that the running projects request failed
export const FAILED_PROJECTS = 'FAILED_PROJECTS'
function failedProjects(error) {
  return {
    type : FAILED_PROJECTS,
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

// Asynchrone action to fetch projects from backend
export function fetchProjects() {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.
  return function (dispatch, getState) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.
    dispatch(requestProjects())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    // Init the authorization bearer of the fetch request
    let authorization = "Basic"
    if ( getState().authentication && getState().authentication.user && getState().authentication.user.access_token){
      authorization = "Bearer " + getState().authentication.user.access_token
    }

    return fetch(PROJECTS_API, {
      headers: {
        'Accept': 'application/json',
        'Authorization': authorization
      }
    })
    .then(checkStatus)
    .then(function(response) {
      return response.json()
    }).then(function(body) {
      dispatch(receiveProjects(body))
    }).catch(function(error) {
      dispatch(failedProjects(error.message))
    })
  }
}

/*********************************************************
***************** IHM related actions ********************
*********************************************************/

// Select a project in the IHM
export const SELECT_PROJECT = 'SELECT_PROJECT'
export function selectProject(id) {
  return {
    type: SELECT_PROJECT,
    id
  }
}

// Add a project to the list
export const ADD_PROJECT = 'ADD_PROJECT'
export function addProject({ id, name }) {
  return {
    type: ADD_PROJECT,
    id,
    name
  }
}

// Delete a project from the list
export const DELETE_PROJECT = 'DELETE_PROJECT'
export function deleteProject(id) {
  return {
    type: DELETE_PROJECT,
    id
  }
}
// Delete a project from the list
export const DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN'
export function deleteProjectAdmin(id) {
  return {
    type: DELETE_PROJECT_ADMIN,
    id
  }
}

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

// Show the admin configuration view
export const SHOW_ADMIN_CONFIGURATION = 'SHOW_ADMIN_CONFIGURATION'
export function showAdminConfiguration() {
  return {
    type: SHOW_ADMIN_CONFIGURATION
  }
}

// Hide the admin configuration view
export const HIDE_ADMIN_CONFIGURATION = 'HIDE_ADMIN_CONFIGURATION'
export function hideAdminConfiguration() {
  return {
    type: HIDE_ADMIN_CONFIGURATION
  }
}
