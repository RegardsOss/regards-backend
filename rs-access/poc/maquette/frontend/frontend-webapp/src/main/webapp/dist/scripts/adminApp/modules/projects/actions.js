"use strict";
const redux_api_middleware_1 = require('redux-api-middleware');
const schemas_1 = require('../../../common/api/schemas');
const normalizr_1 = require('normalizr');
const PROJECTS_API = 'http://localhost:8080/api/projects';
exports.PROJECTS_REQUEST = 'PROJECTS_REQUEST';
exports.PROJECTS_SUCESS = 'PROJECTS_SUCESS';
exports.PROJECTS_FAILURE = 'PROJECTS_FAILURE';
exports.fetchProjects = () => ({
    [redux_api_middleware_1.CALL_API]: {
        types: [
            exports.PROJECTS_REQUEST,
            {
                type: exports.PROJECTS_SUCESS,
                payload: (action, state, res) => redux_api_middleware_1.getJSON(res).then((json) => normalizr_1.normalize(json, schemas_1.default.PROJECT_ARRAY))
            },
            exports.PROJECTS_FAILURE
        ],
        endpoint: PROJECTS_API,
        method: 'GET'
    }
});
exports.ADD_PROJECT = 'ADD_PROJECT';
function addProject(id, name) {
    return {
        type: exports.ADD_PROJECT,
        id,
        name
    };
}
exports.addProject = addProject;
exports.DELETE_PROJECT = 'DELETE_PROJECT';
function deleteProject(id) {
    return {
        type: exports.DELETE_PROJECT,
        id
    };
}
exports.deleteProject = deleteProject;
exports.DELETE_SELECTED_PROJECT = 'DELETE_SELECTED_PROJECT';
function deleteSelectedProject() {
    return {
        type: exports.DELETE_SELECTED_PROJECT
    };
}
exports.deleteSelectedProject = deleteSelectedProject;
//# sourceMappingURL=actions.js.map