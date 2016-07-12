/**
 * Combine all reducers for this module to a single root reducer.
 */
import { combineReducers } from 'redux'
import { MenuReducer } from './MenuReducers'

export default combineReducers({
  menu: MenuReducer
});
