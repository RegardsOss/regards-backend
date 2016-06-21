/**
 * Combine all reducers for this aa to a single root reducer.
 */
import { combineReducers } from 'redux';
// import authentication from '../modules/authentication/reducers/TODO'
// import home from '../modules/home/reducers/TODO'
// import layout from './modules/layout/reducers/MenuReducers'
import projects from './modules/projects/reducers/ProjectsReducers'

export default combineReducers({
  // layout,
  projects
});
