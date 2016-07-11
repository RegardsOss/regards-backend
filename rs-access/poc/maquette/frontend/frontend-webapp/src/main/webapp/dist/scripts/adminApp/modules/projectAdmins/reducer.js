"use strict";
const lodash_1 = require('lodash');
const reducers_1 = require('../../../common/reducers');
const actions_1 = require('./actions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        isFetching: false,
        items: {},
        ids: [],
        lastUpdate: ''
    }, action) => {
    switch (action.type) {
        case actions_1.PROJECT_ADMIN_REQUEST:
            return Object.assign({}, state, { isFetching: true });
        case actions_1.PROJECT_ADMIN_SUCESS:
            return Object.assign({}, state, {
                isFetching: false,
                items: action.payload.entities.projectAdmins,
                ids: lodash_1.union(state.ids, action.payload.result)
            });
        case actions_1.PROJECT_ADMIN_FAILURE:
            return Object.assign({}, state, { isFetching: false });
        case actions_1.UPDATE_OR_CREATE_PROJECT_ADMIN:
            let newState = Object.assign({}, state);
            newState.items[action.id] = {
                id: action.id,
                name: action.name,
                projects: action.projects
            };
            return newState;
        case actions_1.DELETE_PROJECT_ADMIN:
            return reducers_1.deleteEntityReducer(state, action);
        default:
            return state;
    }
};
exports.getProjectAdminById = (state, id) => state.items[id];
exports.getProjectAdminsByProject = (state, project) => lodash_1.values(state.items);
//# sourceMappingURL=reducer.js.map