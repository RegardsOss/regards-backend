import fetch from 'isomorphic-fetch'
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

function checkResponseStatus(response){
  if (response.status === 200){
    return response;
  } else {
    throw new Error(response.statusText);
  }
}

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchPlugins() {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch, getState) {

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

    let authorization = "Basic";
    if ( getState().authentication && getState().authentication.user && getState().authentication.user.access_token){
      authorization = "Bearer " + getState().authentication.user.access_token;
    }

    return fetch(PLUGINS_API, {
      headers: {
        'Accept': 'application/json',
        'Authorization': authorization
      }
    })
    .then(checkResponseStatus)
    .then(function(response) {
      return response.json()
    }).then(function(body) {
      dispatch(receivePlugins(body));
      // Load ech plugins
      body.map( plugin => {
        const paths = plugin.paths.map( path => {
            return window.location.origin + "/plugins/" + path;
        });
        scriptjs(paths, plugin.name);
      });
    }).catch(function(error) {
      dispatch(failedPlugins(error));
    });
  }
}
