"use strict";
const AccessRightsActions_1 = require('./AccessRightsActions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = [], action) => {
    switch (action.type) {
        case AccessRightsActions_1.REQUEST_ACCESSRIGHTS:
            return state;
        case AccessRightsActions_1.RECEIVE_ACCESSRIGHTS:
        case AccessRightsActions_1.FAILED_ACCESSRIGHTS:
            return [...state, {
                    name: action.view,
                    access: action.access
                }];
        default:
            return state;
    }
};
//# sourceMappingURL=AccessRightsReducers.js.map