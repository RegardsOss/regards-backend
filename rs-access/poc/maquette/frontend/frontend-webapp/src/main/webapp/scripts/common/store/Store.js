import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware } from 'redux';
import themeReducers from 'common/theme/ThemeReducers';
import pluginReducers from 'common/pluginsManager/PluginReducers';
import moduleReducers from 'common/modulesManager/RegardsModulesReducers';
import authenticateReducers from 'common/authentication/AuthenticateReducers';
import projectsReducers from 'portalApp/projects/ProjectsReducers';

// Create the compined reducers by adding all modules reducers
const allReducers = Object.assign({}, themeReducers,
  pluginReducers, moduleReducers, projectsReducers,
  authenticateReducers);
const reducers = combineReducers(allReducers);

// Default store values
const defaultStore = {
  theme: '',
  plugins : [],
  pluginsLoaded: false,
  views : [],
  authentication : {},
  projects : {}
}

// Create the application store
const store = createStore(
  reducers,
  defaultStore,
  applyMiddleware(
    thunkMiddleware
  )
);

const render = () => {
  console.log("Store updated : ",store.getState());
}
store.subscribe(render);

export default store
