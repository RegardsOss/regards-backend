/**
 * Combine all reducers for this app to a single root reducer.
 */
import { combineReducers } from 'redux';
// import authentication from '../modules/authentication/reducers/TODO'
// import home from '../modules/home/reducers/TODO'
// import layout from './modules/layout/reducers/MenuReducers'
import projects from './modules/projects/reducer'
import projectAdmins from './modules/projectAdmins/reducer'
import ui from './modules/ui/reducer'

export default combineReducers({
  // layout,
  projects,
  projectAdmins,
  ui
});
