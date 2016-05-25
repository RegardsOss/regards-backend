
import { createStore, combineReducers } from 'redux';
import commonReducers from './commonReducers';
import pluginReducers from '../Common/PluginsManager/PluginReducers';

const allReducers = Object.assign({},commonReducers,pluginReducers);
const reducers = combineReducers(allReducers);


const defaultStore = {
  application: "UNDEFINED",
  plugins : [],
  pluginsLoaded: false
}

let store = createStore(reducers, defaultStore);
const render = () => {
  console.log("Store updated : ",store.getState());
}
let unsubscribe = store.subscribe(render);

const resetStore = (defaultValue) => {
  unsubscribe();
  if (defaultValue){
    store = createStore(reducers, defaultValue);
  } else {
    store = createStore(reducers, defaultStore);
  }
  unsubscribe = store.subscribe(render);
  render();
}

export { createStore }
export default store
