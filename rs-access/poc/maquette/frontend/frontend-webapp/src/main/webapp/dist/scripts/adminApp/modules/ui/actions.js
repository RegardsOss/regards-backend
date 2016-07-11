"use strict";
exports.SHOW_PROJECT_CONFIGURATION = 'SHOW_PROJECT_CONFIGURATION';
function showProjectConfiguration() {
    return {
        type: exports.SHOW_PROJECT_CONFIGURATION
    };
}
exports.showProjectConfiguration = showProjectConfiguration;
exports.HIDE_PROJECT_CONFIGURATION = 'HIDE_PROJECT_CONFIGURATION';
function hideProjectConfiguration() {
    return {
        type: exports.HIDE_PROJECT_CONFIGURATION
    };
}
exports.hideProjectConfiguration = hideProjectConfiguration;
exports.SELECT_PROJECT = 'SELECT_PROJECT';
function selectProject(id) {
    return {
        type: exports.SELECT_PROJECT,
        id
    };
}
exports.selectProject = selectProject;
exports.DELETE_PROJECT = 'DELETE_PROJECT';
function deleteProject(id) {
    return {
        type: exports.DELETE_PROJECT,
        id
    };
}
exports.deleteProject = deleteProject;
exports.SELECT_PROJECT_ADMIN = 'SELECT_PROJECT_ADMIN';
function selectProjectAdmin(id) {
    return {
        type: exports.SELECT_PROJECT_ADMIN,
        id
    };
}
exports.selectProjectAdmin = selectProjectAdmin;
exports.DELETE_PROJECT_ADMIN = 'DELETE_PROJECT_ADMIN';
function deleteProjectAdmin(id) {
    return {
        type: exports.DELETE_PROJECT_ADMIN,
        id
    };
}
exports.deleteProjectAdmin = deleteProjectAdmin;
exports.SHOW_PROJECT_ADMIN_CONFIGURATION = 'SHOW_PROJECT_ADMIN_CONFIGURATION';
function showProjectAdminConfiguration() {
    return {
        type: exports.SHOW_PROJECT_ADMIN_CONFIGURATION
    };
}
exports.showProjectAdminConfiguration = showProjectAdminConfiguration;
exports.HIDE_PROJECT_ADMIN_CONFIGURATION = 'HIDE_PROJECT_ADMIN_CONFIGURATION';
function hideProjectAdminConfiguration() {
    return {
        type: exports.HIDE_PROJECT_ADMIN_CONFIGURATION
    };
}
exports.hideProjectAdminConfiguration = hideProjectAdminConfiguration;
//# sourceMappingURL=actions.js.map