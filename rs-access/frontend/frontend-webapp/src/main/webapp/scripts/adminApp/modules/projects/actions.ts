const {CALL_API, getJSON} = require ('redux-api-middleware')
import Schemas from "../../../common/api/schemas";
import { normalize } from "normalizr";
import { Action } from "redux";

export const PROJECTS_API = 'http://localhost:8080/api/projects'
export const PROJECTS_REQUEST = 'PROJECTS_REQUEST'
export const PROJECTS_SUCESS = 'PROJECTS_SUCESS'
export const PROJECTS_FAILURE = 'PROJECTS_FAILURE'

// Fetches all projects
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
export const fetchProjects = () => ({
  [CALL_API]: {
    types: [
      PROJECTS_REQUEST,
      {
        type: PROJECTS_SUCESS,
        payload: (action: any, state: any, res: any) => getJSON (res).then ((json: any) => normalize (json, Schemas.PROJECT_ARRAY))
      },
      PROJECTS_FAILURE
    ],
    endpoint: PROJECTS_API,
    method: 'GET'
  }
})

export interface ProjectAction extends Action {
  id: string,
  name: string
}

// Add a project to the list
export const ADD_PROJECT = 'ADD_PROJECT'

export const addProject = (id: string, name: string): ProjectAction => ({
  type: ADD_PROJECT,
  id,
  name
})

export const DELETE_PROJECT = 'DELETE_PROJECT'
export function deleteProject(id: string): Object {
  return {
    type: DELETE_PROJECT,
    id
  }
}

export const DELETE_SELECTED_PROJECT = 'DELETE_SELECTED_PROJECT'
export function deleteSelectedProject(): Object {
  return {
    type: DELETE_SELECTED_PROJECT
  }
}
