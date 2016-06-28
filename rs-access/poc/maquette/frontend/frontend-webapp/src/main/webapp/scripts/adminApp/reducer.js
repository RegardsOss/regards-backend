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

export default combineReducers({
  // layout,
  projects,
  projectAdmins,
  ui
});


// Selectors
export const getProjects = (state)  =>
  fromProjects.getProjects(state.adminApp.projects)

export const getProjectById = (state, id) =>
  fromProjects.getProjectById(state.adminApp.projects, id)

export const getSelectedProjectId = (state) =>
  fromUi.getSelectedProjectId(state.adminApp.ui)

export const getSelectedProjectAdminId = (state) =>
  fromUi.getSelectedProjectAdminId(state.adminApp.ui)

export const getProjectAdmins = (state) =>
  state.adminApp.projectAdmins

export const getProjectAdminById = (state, id) =>
  fromProjectAdmins.getProjectAdminById(state.adminApp.projectAdmins, id)

export const getProjectAdminsByProject = (state, project) =>
  fromProjectAdmins.getProjectAdminsByProject(state.adminApp.projectAdmins, project)
