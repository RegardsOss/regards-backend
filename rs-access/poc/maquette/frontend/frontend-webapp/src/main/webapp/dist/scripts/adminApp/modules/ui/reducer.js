"use strict";
const actions_1 = require('./actions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = [], action) => {
    switch (action.type) {
        case actions_1.SELECT_PROJECT:
            return Object.assign({}, state, { selectedProjectId: action.id });
        case actions_1.SELECT_PROJECT_ADMIN:
            return Object.assign({}, state, { selectedProjectAdminId: action.id });
        case actions_1.SHOW_PROJECT_CONFIGURATION:
            return Object.assign({}, state, {
                projectConfigurationIsShown: true,
                projectAdminConfigurationIsShown: false
            });
        case actions_1.HIDE_PROJECT_CONFIGURATION:
            return Object.assign({}, state, { projectConfigurationIsShown: false });
        case actions_1.SHOW_PROJECT_ADMIN_CONFIGURATION:
            return Object.assign({}, state, {
                projectConfigurationIsShown: false,
                projectAdminConfigurationIsShown: true
            });
        case actions_1.HIDE_PROJECT_ADMIN_CONFIGURATION:
            return Object.assign({}, state, { projectAdminConfigurationIsShown: false });
        default:
            return state;
    }
};
exports.getSelectedProjectId = (state) => state.selectedProjectId;
exports.getSelectedProjectAdminId = (state) => state.selectedProjectAdminId;
//# sourceMappingURL=reducer.js.map