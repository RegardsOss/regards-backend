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
import RoleReducer from "./modules/userManagement/role.reducer"
import ProjectAccountReducers from "./modules/userManagement/projectAccount.reducer"
import * as ProjectAccountsSelectors from "./modules/userManagement/projectAccount.reducer"
import AccountReducers from "./modules/userManagement/account.reducer"
import * as AccountsSelectors from "./modules/userManagement/account.reducer"
import {
  DatasetFormReducer,
  DatasetFormSelectors,
  DatasetReducer,
  DatasetSelectors,
  ModelReducer,
  ModelSelectors
} from "./modules/datamanagement/reducer"


const forms = combineReducers({
  createDataset: DatasetFormReducer
})

export default combineReducers({
  projects,
  projectAdmins,
  ui,
  roles: RoleReducer,
  accounts: AccountReducers,
  projectAccounts: ProjectAccountReducers,
  model: ModelReducer,
  dataset: DatasetReducer,
  forms,
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

export const getProjectAccounts = (state: any) =>
  ProjectAccountsSelectors.getProjectAccounts(state.adminApp.projectAccounts)

export const getProjectAccountById = (state: any, projectAccountId: number) =>
  ProjectAccountsSelectors.getProjectAccountById(state.adminApp.projectAccounts, projectAccountId)

export const getAccounts = (state: any) =>
  AccountsSelectors.getAccounts(state.adminApp.accounts)

export const getAccountById = (state: any, accountId: number) =>
  AccountsSelectors.getAccountById(state.adminApp.accounts, accountId)

export const getFormDatasetViewState = (state: any) =>
  DatasetFormSelectors.getFormDatasetViewState(state.adminApp.forms.createDataset)

export const getModels = (state: any) =>
  ModelSelectors.getModel(state.adminApp.model)

export const getDatasets = (state: any) =>
  DatasetSelectors.getDatasets(state.adminApp.dataset)
