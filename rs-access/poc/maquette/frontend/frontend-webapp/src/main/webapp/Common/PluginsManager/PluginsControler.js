
import { Rest } from 'grommet';
import scriptjs from 'scriptjs';

import store from 'AppStore';
import { addPlugin, pluginsLoaded, togglePluginsLoaded } from './PluginsStoreActionCreators';

const loadPlugins = (callback) => {
  // Get plugins from server
  const location = window.location.origin + '/json/plugins.json';
  return Rest.get(location)
    .end((error, response) => {
      if (response.status === 200){

        // Add event listenner for plugin to load
        document.addEventListener('plugin', function (plugin) {
          // When plugin loaded event is received, add plugin to the store
          if (callback){
            callback(plugin.detail);
          }
          store.dispatch(addPlugin(plugin.detail.name,plugin.detail.app));
        },false);

          // Check if there is plugins to load
          if (response.body.plugins && response.body.plugins.length > 0){
            console.log(response.body.plugins.length + " plugins to load ...");

            // Load each plugin
            let index=0;
            let error=0;
            response.body.plugins.map( plugin => {
              console.log("loading ", plugin.name);
              if (plugin.paths) {
                const paths = plugin.paths.map( path => {
                    return window.location.origin + "/Plugins/" + path;
                });
                scriptjs(paths, plugin.name, ()=> {
                  // Check if plugin is successfully loaded in store
                  index++;
                  if (store.getState().plugins.length !== (index - error) ){
                    console.log("Error loading plugin "+ plugin.name);
                    error++;
                  }

                  // After last plugin loaded, set pluginsLoaded to true in store
                  if (index === response.body.plugins.length){
                    // Every plugins is loaded add the state to the store
                    console.log("All Plugins loaded");
                    store.dispatch(pluginsLoaded());
                  }
                });
              }
            });
          }
      } else {
        console.log("Error while loading plugins");
        store.dispatch(pluginsLoaded());
      }
    });
  }

export { loadPlugins }
