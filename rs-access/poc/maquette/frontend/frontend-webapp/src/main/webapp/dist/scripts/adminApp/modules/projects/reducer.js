"use strict";
const lodash_1 = require('lodash');
const reducers_1 = require('../../common/reducers');
const actions_1 = require('./actions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        isFetching: false,
        items: [],
        ids: [],
        lastUpdate: ''
    }, action) => {
    switch (action.type) {
        case actions_1.PROJECTS_REQUEST:
            return Object.assign({}, state, { isFetching: true });
        case actions_1.PROJECTS_SUCESS:
            return Object.assign({}, state, {
                isFetching: false,
                items: action.payload.entities.projects,
                ids: lodash_1.union(state.ids, action.payload.result)
            });
        case actions_1.PROJECTS_FAILURE:
            return Object.assign({}, state, { isFetching: false });
        case actions_1.ADD_PROJECT:
            let newState = Object.assign({}, state);
            newState.items[action.id] = {
                name: action.name,
                links: []
            };
            newState.ids.push(action.id);
            return newState;
        // return { ...state,
        //   items: state.items.concat({
        //     id: action.id,
        //     name: action.name,
        //     selected: false,
        //     admins: []
        //   })
        // }
        case actions_1.DELETE_PROJECT:
            return reducers_1.deleteEntityReducer(state, action);
        default:
            return state;
    }
};
// Selectors
exports.getProjects = (state) => state;
exports.getProjectById = (state, id) => state.items[id];
//# sourceMappingURL=reducer.js.map