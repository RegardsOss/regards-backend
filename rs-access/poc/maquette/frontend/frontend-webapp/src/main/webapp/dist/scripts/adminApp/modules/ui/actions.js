"use strict";
// Show the project configuration view
exports.SHOW_PROJECT_CONFIGURATION = 'SHOW_PROJECT_CONFIGURATION';
function showProjectConfiguration() {
    return {
        type: exports.SHOW_PROJECT_CONFIGURATION
    };
}
exports.showProjectConfiguration = showProjectConfiguration;
// Hide the project configuration view
exports.HIDE_PROJECT_CONFIGURATION = 'HIDE_PROJECT_CONFIGURATION';
function hideProjectConfiguration() {
    return {
        type: exports.HIDE_PROJECT_CONFIGURATION
    };
}
exports.hideProjectConfiguration = hideProjectConfiguration;
// Select a project in the IHM
exports.SELECT_PROJECT = 'SELECT_PROJECT';
function selectProject(id) {
    return {
        type: exports.SELECT_PROJECT,
        id: id
    };
}
exports.selectProject = selectProject;
// Delete a project from the list
exports.DELETE_PROJECT = 'DELETE_PROJECT';
function deleteProject(id) {
    return {
        type: exports.DELETE_PROJECT,
        id: id
    };
}
exports.deleteProject = deleteProject;
// Set active a project admin in the IHM
exports.SELECT_PROJECT_ADMIN = 'SELECT_PROJECT_ADMIN';
function selectProjectAdmin(id) {
    return {
        type: exports.SELECT_PROJECT_ADMIN,
        id: id
    };
}
exports.selectProjectAdmin = selectProjectAdmin;
// Delete a project from the list
exports.DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN';
function deleteProjectAdmin(id) {
    return {
        type: exports.DELETE_PROJECT_ADMIN,
        id: id
    };
}
exports.deleteProjectAdmin = deleteProjectAdmin;
// Show the admin configuration view
exports.SHOW_PROJECT_ADMIN_CONFIGURATION = 'SHOW_PROJECT_ADMIN_CONFIGURATION';
function showProjectAdminConfiguration() {
    return {
        type: exports.SHOW_PROJECT_ADMIN_CONFIGURATION
    };
}
exports.showProjectAdminConfiguration = showProjectAdminConfiguration;
// Hide the admin configuration view
exports.HIDE_PROJECT_ADMIN_CONFIGURATION = 'HIDE_PROJECT_ADMIN_CONFIGURATION';
function hideProjectAdminConfiguration() {
    return {
        type: exports.HIDE_PROJECT_ADMIN_CONFIGURATION
    };
}
exports.hideProjectAdminConfiguration = hideProjectAdminConfiguration;
//# sourceMappingURL=actions.js.map