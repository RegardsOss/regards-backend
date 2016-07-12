"use strict";
const MenuActions_1 = require('../actions/MenuActions');
const AuthenticateActions_1 = require('../../../../common/authentication/AuthenticateActions');
exports.MenuReducer = (state, action) => {
    switch (action.type) {
        case AuthenticateActions_1.LOGOUT:
            console.log("Logging out");
            return state;
        case MenuActions_1.SHOW_PROJECTS:
            console.log("Showing projects");
            return state;
        default:
            return state;
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = exports.MenuReducer;
//# sourceMappingURL=MenuReducers.js.map