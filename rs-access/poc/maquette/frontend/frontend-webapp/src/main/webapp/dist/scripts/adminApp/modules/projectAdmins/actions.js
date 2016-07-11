"use strict";
var { CALL_API, getJSON } = require('redux-api-middleware');
const schemas_1 = require('../../../common/api/schemas');
const normalizr_1 = require('normalizr');
const PROJECT_ADMINS_API = 'http://localhost:8080/api/project-admins';
exports.PROJECT_ADMIN_REQUEST = 'PROJECT_ADMIN_REQUEST';
exports.PROJECT_ADMIN_SUCESS = 'PROJECT_ADMIN_SUCESS';
exports.PROJECT_ADMIN_FAILURE = 'PROJECT_ADMIN_FAILURE';
// Fetches all project admins
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
exports.fetchProjectAdmins = () => ({
    [CALL_API]: {
        // endpointKey : key,
        // links: dataObject.links,
        types: [
            exports.PROJECT_ADMIN_REQUEST,
            {
                type: exports.PROJECT_ADMIN_SUCESS,
                payload: (action, state, res) => getJSON(res).then((json) => normalizr_1.normalize(json, schemas_1.default.PROJECT_ADMIN_ARRAY))
            },
            exports.PROJECT_ADMIN_FAILURE
        ],
        endpoint: PROJECT_ADMINS_API,
        method: 'GET'
    }
});
// Fetches all project admins
// Relies on the custom API middleware defined in redux-api-middleware
// Normalize the json response
exports.fetchProjectAdminsBy = (endpoint) => ({
    [CALL_API]: {
        // endpointKey : key,
        // links: dataObject.links,
        types: [
            exports.PROJECT_ADMIN_REQUEST,
            {
                type: exports.PROJECT_ADMIN_SUCESS,
                payload: (action, state, res) => getJSON(res).then((json) => normalizr_1.normalize(json, schemas_1.default.PROJECT_ADMIN_ARRAY))
            },
            exports.PROJECT_ADMIN_FAILURE
        ],
        endpoint: endpoint,
        method: 'GET'
    }
});
// Delete a project admins from the list
exports.DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN';
function deleteProjectAdmin(id) {
    return {
        type: exports.DELETE_PROJECT_ADMIN,
        id: id
    };
}
exports.deleteProjectAdmin = deleteProjectAdmin;
exports.UPDATE_PROJECT_ADMIN = 'UPDATE_PROJECT_ADMIN';
function updateProjectAdmin(projectAdmin) {
    return {
        type: exports.UPDATE_PROJECT_ADMIN,
        projectAdmin: projectAdmin
    };
}
exports.updateProjectAdmin = updateProjectAdmin;
/**
 * [UPDATE_OR_CREATE_PROJECT_ADMIN description]
 * @type {String} id of the project admin to update/create
 * @type {String} name of the project admin to update/create
 * @type {String} list to projects ids to associate the project admin to
 */
exports.UPDATE_OR_CREATE_PROJECT_ADMIN = 'UPDATE_OR_CREATE_PROJECT_ADMIN';
function updateOrCreateProjectAdmin(id, name, projects) {
    return {
        type: exports.UPDATE_OR_CREATE_PROJECT_ADMIN,
        id: id,
        name: name,
        projects: projects
    };
}
exports.updateOrCreateProjectAdmin = updateOrCreateProjectAdmin;
//# sourceMappingURL=actions.js.map