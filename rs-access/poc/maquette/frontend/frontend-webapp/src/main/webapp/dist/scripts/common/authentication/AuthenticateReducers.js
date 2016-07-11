"use strict";
const AuthenticateActions_1 = require('./AuthenticateActions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        isFetching: false,
        user: {},
        authenticateDate: '',
        error: ''
    }, action) => {
    switch (action.type) {
        case AuthenticateActions_1.REQUEST_AUTHENTICATE:
            return Object.assign({}, state, {
                isFetching: true
            });
        case AuthenticateActions_1.RECEIVE_AUTHENTICATE:
            return Object.assign({}, state, {
                isFetching: false,
                user: action.user,
                authenticateDate: action.authenticateDate
            });
        case AuthenticateActions_1.FAILED_AUTHENTICATE:
            return Object.assign({}, state, {
                isFetching: false,
                error: action.error
            });
        case AuthenticateActions_1.LOGOUT:
            return {
                isFetching: false,
                user: {},
                authenticateDate: '',
                error: ''
            };
        default:
            return state;
    }
};
//# sourceMappingURL=AuthenticateReducers.js.map