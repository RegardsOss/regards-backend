"use strict";
/**
 * Combine all reducers for this module to a single root reducer.
 */
const redux_1 = require('redux');
const MenuReducers_1 = require('./MenuReducers');
module.exports = redux_1.combineReducers({
    menu: MenuReducers_1.MenuReducer
});
//# sourceMappingURL=index.js.map