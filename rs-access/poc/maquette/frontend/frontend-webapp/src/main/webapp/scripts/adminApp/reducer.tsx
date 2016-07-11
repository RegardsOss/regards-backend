/**
 * Combine all reducers for this app to a single root reducer.
 */
import { combineReducers } from 'redux';
// import authentication from '../modules/authentication/reducers/TODO'
// import home from '../modules/home/reducers/TODO'
// import layout from './modules/layout/reducers/MenuReducers'
import projects, * as fromProjects from './modules/projects/reducer'
import projectAdmins, * as fromProjectAdmins from './modules/projectAdmins/reducer'
import ui, * as fromUi from './modules/ui/reducer'
import { values } from 'lodash'

export default combineReducers({
  // layout,
  projects,
  projectAdmins,
  ui
});


// Selectors
export const getProjects = (state: any)  =>
  fromProjects.getProjects(state.adminApp.projects)

export const getProjectById = (state: any, id: string) =>
  fromProjects.getProjectById(state.adminApp.projects, id)

export const getSelectedProjectId = (state: any) =>
  fromUi.getSelectedProjectId(state.adminApp.ui)

export const getSelectedProjectAdminId = (state: any) =>
  fromUi.getSelectedProjectAdminId(state.adminApp.ui)

export const getProjectAdmins = (state: any) =>
  state.adminApp.projectAdmins

export const getProjectAdminById = (state: any, id: string) =>
  fromProjectAdmins.getProjectAdminById(state.adminApp.projectAdmins, id)

export const getProjectAdminsByProject = (state: any, project: string) =>
  fromProjectAdmins.getProjectAdminsByProject(state.adminApp.projectAdmins, project)
