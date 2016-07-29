var {CALL_API} = require ('redux-api-middleware')

// Backend api adress
export const PROJECTS_API = 'http://localhost:8080/api/projects'
// Action to inform store that te projects request is running
export const REQUEST_PROJECTS = 'REQUEST_PROJECTS'
// Action to inform store that te projects are availables
export const RECEIVE_PROJECTS = 'RECEIVE_PROJECTS'
// Action to inform store that the running projects request failed
export const FAILED_PROJECTS = 'FAILED_PROJECTS'

export const fetchProjects = () => ({
  [CALL_API]: {
    types: [
      REQUEST_PROJECTS,
      {
        type: RECEIVE_PROJECTS,
        meta: {receivedAt: Date.now ()}
      },
      FAILED_PROJECTS
    ],
    endpoint: PROJECTS_API,
    method: 'GET'
  }
})
