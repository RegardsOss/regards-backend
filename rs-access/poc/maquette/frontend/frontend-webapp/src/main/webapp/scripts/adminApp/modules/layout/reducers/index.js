/**
 * Combine all reducers for this module to a single root reducer.
 */
import { combineReducers } from 'redux';

const reducers = {
  menu: require('../reducers/MenuReducers.js') 
};
module.exports = combineReducers(reducers);
