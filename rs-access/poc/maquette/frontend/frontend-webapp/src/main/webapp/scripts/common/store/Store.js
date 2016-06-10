import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware } from 'redux';

import loggerMiddleware from 'common/logger/ActionLoggerMiddleware';

import themeReducers from 'common/theme/reducers/ThemeReducers';
import pluginReducers from 'common/plugins/PluginReducers';
import accessRightsReducers from 'common/access-rights/AccessRightsReducers';
import authenticateReducers from 'common/authentication/AuthenticateReducers';
import projectsReducers from 'portalApp/modules/projects/reducers/ProjectsReducers';

// Create the compined reducers by adding all modules reducers
const allReducers = Object.assign({}, themeReducers,
  pluginReducers, accessRightsReducers, projectsReducers,
  authenticateReducers);
const reducers = combineReducers(allReducers);

// Default store values
const defaultStore = {
  theme: '',
  plugins : {},
  views : [],
  authentication : {},
  projects : {}
}

// Create the application store
const store = createStore(
  reducers,
  defaultStore,
  applyMiddleware(
    thunkMiddleware, // lets us dispatch() functions
    loggerMiddleware // logs any dispatched action
  )
);

// Log any change in the store
const render = () => {
  console.log("STORE UPDATED : ",store.getState());
}
store.subscribe(render);

export default store
