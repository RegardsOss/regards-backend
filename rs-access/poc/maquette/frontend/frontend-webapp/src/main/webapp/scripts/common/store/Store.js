import { createStore, combineReducers } from 'redux';
import commonReducers from './CommonReducers';
import pluginReducers from 'common/pluginsManager/PluginReducers';
import moduleReducers from 'common/modulesManager/RegardsModulesReducers';

// Create the compined reducers by adding all modules reducers
const allReducers = Object.assign({},commonReducers,pluginReducers,moduleReducers);
const reducers = combineReducers(allReducers);

// Default store values
const defaultStore = {
  theme: '',
  plugins : [],
  pluginsLoaded: false,
  views : [],
  authenticated: false
}

// Create the application store
const store = createStore(reducers, defaultStore);
const render = () => {
  console.log("Store updated : ",store.getState());
}
store.subscribe(render);

export default store
