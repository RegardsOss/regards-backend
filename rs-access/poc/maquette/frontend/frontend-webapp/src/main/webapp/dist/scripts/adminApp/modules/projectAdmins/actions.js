"use strict";
var { CALL_API, getJSON } = require('redux-api-middleware');
const schemas_1 = require('../../../common/api/schemas');
const normalizr_1 = require('normalizr');
const PROJECT_ADMINS_API = 'http://localhost:8080/api/project-admins';
exports.PROJECT_ADMIN_REQUEST = 'PROJECT_ADMIN_REQUEST';
exports.PROJECT_ADMIN_SUCESS = 'PROJECT_ADMIN_SUCESS';
exports.PROJECT_ADMIN_FAILURE = 'PROJECT_ADMIN_FAILURE';
exports.fetchProjectAdmins = () => ({
    [CALL_API]: {
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
exports.fetchProjectAdminsBy = (endpoint) => ({
    [CALL_API]: {
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
exports.DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN';
function deleteProjectAdmin(id) {
    return {
        type: exports.DELETE_PROJECT_ADMIN,
        id
    };
}
exports.deleteProjectAdmin = deleteProjectAdmin;
exports.UPDATE_PROJECT_ADMIN = 'UPDATE_PROJECT_ADMIN';
function updateProjectAdmin(projectAdmin) {
    return {
        type: exports.UPDATE_PROJECT_ADMIN,
        projectAdmin
    };
}
exports.updateProjectAdmin = updateProjectAdmin;
exports.UPDATE_OR_CREATE_PROJECT_ADMIN = 'UPDATE_OR_CREATE_PROJECT_ADMIN';
function updateOrCreateProjectAdmin(id, name, projects) {
    return {
        type: exports.UPDATE_OR_CREATE_PROJECT_ADMIN,
        id,
        name,
        projects
    };
}
exports.updateOrCreateProjectAdmin = updateOrCreateProjectAdmin;
//# sourceMappingURL=actions.js.map