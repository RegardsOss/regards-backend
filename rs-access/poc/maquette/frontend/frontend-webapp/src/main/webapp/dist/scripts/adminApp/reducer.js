"use strict";
/**
 * Combine all reducers for this app to a single root reducer.
 */
const redux_1 = require('redux');
// import authentication from '../modules/authentication/reducers/TODO'
// import home from '../modules/home/reducers/TODO'
// import layout from './modules/layout/reducers/MenuReducers'
const reducer_1 = require('./modules/projects/reducer'), fromProjects = reducer_1;
const reducer_2 = require('./modules/projectAdmins/reducer'), fromProjectAdmins = reducer_2;
const reducer_3 = require('./modules/ui/reducer'), fromUi = reducer_3;
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = redux_1.combineReducers({
    // layout,
    projects: reducer_1.default,
    projectAdmins: reducer_2.default,
    ui: reducer_3.default
});
// Selectors
exports.getProjects = (state) => fromProjects.getProjects(state.adminApp.projects);
exports.getProjectById = (state, id) => fromProjects.getProjectById(state.adminApp.projects, id);
exports.getSelectedProjectId = (state) => fromUi.getSelectedProjectId(state.adminApp.ui);
exports.getSelectedProjectAdminId = (state) => fromUi.getSelectedProjectAdminId(state.adminApp.ui);
exports.getProjectAdmins = (state) => state.adminApp.projectAdmins;
exports.getProjectAdminById = (state, id) => fromProjectAdmins.getProjectAdminById(state.adminApp.projectAdmins, id);
exports.getProjectAdminsByProject = (state, project) => fromProjectAdmins.getProjectAdminsByProject(state.adminApp.projectAdmins, project);
//# sourceMappingURL=reducer.js.map