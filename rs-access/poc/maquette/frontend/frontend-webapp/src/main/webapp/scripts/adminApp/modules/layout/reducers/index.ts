/**
 * Combine all reducers for this module to a single root reducer.
 */
import { combineReducers } from 'redux'
import { MenuReducer } from './MenuReducers'

module.exports = combineReducers({
  menu: MenuReducer
});
