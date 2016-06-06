import Rest from 'grommet/utils/Rest';
import scriptjs from 'scriptjs';

const PLUGINS_API='http://localhost:8080/api/plugins';

export const REQUEST_PLUGINS = 'REQUEST_PLUGINS'
function requestPlugins() {
  return {
    type: REQUEST_PLUGINS,
  }
}

export const RECEIVE_PLUGINS = 'RECEIVE_PLUGINS'
function receivePlugins(plugins) {
  return {
    type: RECEIVE_PLUGINS,
    plugins: plugins,
    receivedAt: Date.now()
  }
}

export const FAILED_PLUGINS = 'FAILED_PLUGINS';
function failedPlugins(error) {
  return {
    type : FAILED_PLUGINS,
    error : error
  }
}

export const PLUGIN_INITIALIZED = 'PLUGIN_INITIALIZED'
function pluginInitialized(name, plugin){
  return {
    type: PLUGIN_INITIALIZED,
    name: name,
    plugin: plugin,
    error: ''
  }
}

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchPlugins() {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch) {

    document.addEventListener('plugin', function (plugin) {
      // When plugin loaded event is received, add plugin to the store
      dispatch(pluginInitialized(plugin.detail.name,plugin.detail.app));
    },false);

    // First dispatch: the app state is updated to inform
    // that the API call is starting.

    dispatch(requestPlugins())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    return Rest.get(PLUGINS_API)
      .end((error, response) => {

        if (response.status === 200){
          dispatch(receivePlugins(response.body));
          // Load ech plugins
          response.body.map( plugin => {
            const paths = plugin.paths.map( path => {
                return window.location.origin + "/scripts/plugins/" + path;
            });
            scriptjs(paths, plugin.name);
          });
        } else {
          dispatch(failedPlugins(error));
        }
    });
  }
}
