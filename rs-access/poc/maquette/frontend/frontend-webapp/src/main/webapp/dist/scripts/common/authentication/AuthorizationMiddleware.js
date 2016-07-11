"use strict";
var { CALL_API } = require('redux-api-middleware');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (store) => (next) => (action) => {
    let callAPI = action[CALL_API];
    if (callAPI)
        callAPI["headers"] = (store) => ({ 'Accept': 'application/json', 'Authorization': getAuthorization(store) || '' });
    return next(action);
};
const getAuthorization = (state) => {
    // Init the authorization bearer of the fetch request
    let authentication = state.common.authentication;
    let authorization = "Basic";
    if (authentication && authentication.user && authentication.user.access_token) {
        authorization = "Bearer " + authentication.user.access_token;
    }
    return authorization;
};
//# sourceMappingURL=AuthorizationMiddleware.js.map