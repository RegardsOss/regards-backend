"use strict";
const ProjectsActions_1 = require('../actions/ProjectsActions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        isFetching: false,
        items: [],
        lastUpdate: ''
    }, action) => {
    switch (action.type) {
        case ProjectsActions_1.REQUEST_PROJECTS:
            return Object.assign({}, state, {
                isFetching: true
            });
        case ProjectsActions_1.RECEIVE_PROJECTS:
            return Object.assign({}, state, {
                isFetching: false,
                items: action.projects,
                lastUpdate: action.receivedAt
            });
        case ProjectsActions_1.FAILED_PROJECTS:
            return Object.assign({}, state, {
                isFetching: false
            });
        default:
            return state;
    }
};
//# sourceMappingURL=ProjectsReducers.js.map