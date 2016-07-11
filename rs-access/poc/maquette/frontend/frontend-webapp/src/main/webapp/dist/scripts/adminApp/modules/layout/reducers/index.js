"use strict";
/**
 * Combine all reducers for this module to a single root reducer.
 */
const redux_1 = require('redux');
const reducers = {
    menu: require('../reducers/MenuReducers.js')
};
module.exports = redux_1.combineReducers(reducers);
//# sourceMappingURL=index.js.map