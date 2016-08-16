/**
 * Combine all reducers for this app to a single root reducer.
 */
import { combineReducers } from "redux"
import projects from "./modules/projects/reducer"
import * as fromProjects from "./modules/projects/reducer"
import projectAdmins from "./modules/projectAdmins/reducer"
import * as fromProjectAdmins from "./modules/projectAdmins/reducer"
import ui from "./modules/ui/reducer"
import * as fromUi from "./modules/ui/reducer"
import * as UserSelectors from "./modules/projectUsers/user.reducer"
import RoleReducer from "./modules/projectUsers/role.reducer"
import UsersReducers from "./modules/userManagement/reducer.ts"
// import * as RoleSelectors from "./modules/projectUsers/role.reducer"
// import * as ProjectUserSelectors from "./modules/projectUsers/projectUser.reducer"
// import authentication from '../modules/authentication/reducers/TODO'
// import home from '../modules/home/reducers/TODO'
//


export default combineReducers({
  projects,
  projectAdmins,
  ui,
  roles: RoleReducer,
  users: UsersReducers
})

// WIP
// const selectors = {
//   projects: fromProjects,
//   projectAdmins: fromProjectAdmins,
//   ui: fromUi
// }
// export const get = (state: any, element: string) =>
//   state.adminApp[element]
//
// export const getBy = (state: any, element: string, property: string, propertyValue: any) =>
//   selectors[element]['getBy'+property](state.adminApp[element], propertyValue)

// Selectors
export const getProjects = (state: any) =>
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

export const getUserLinks = (state: any) =>
  UserSelectors.getProjectUsersId(state.adminApp.users)

export const getUsersById = (state: any, userLink: string) =>
  UserSelectors.getUsersById(state.adminApp.users, userLink)
