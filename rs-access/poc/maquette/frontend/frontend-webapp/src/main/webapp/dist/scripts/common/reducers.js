"use strict";
const redux_1 = require('redux');
const ThemeReducers_1 = require('./theme/reducers/ThemeReducers');
const PluginReducers_1 = require('./plugins/PluginReducers');
const AccessRightsReducers_1 = require('./access-rights/AccessRightsReducers');
const AuthenticateReducers_1 = require('./authentication/AuthenticateReducers'), fromAuthentication = AuthenticateReducers_1;
const lodash_1 = require('lodash');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = redux_1.combineReducers({
    theme: ThemeReducers_1.default,
    plugins: PluginReducers_1.default,
    views: AccessRightsReducers_1.default,
    authentication: AuthenticateReducers_1.default
});
exports.deleteEntityReducer = (state, action) => (Object.assign({}, state, {
    items: lodash_1.pick(state.items, (value, key) => key !== action.id),
    ids: state.ids.filter((id) => id !== action.id)
}));
//# sourceMappingURL=reducers.js.map