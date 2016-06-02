
import { Rest } from 'grommet';
import scriptjs from 'scriptjs';

import store from 'AppStore';
import { addPlugin, pluginsLoaded, togglePluginsLoaded } from './PluginsStoreActionCreators';

const loadPlugins = (callback) => {
  // Get plugins from server
  const location = 'http://localhost:8080/api/plugins';
  return Rest.get(location)
    .end((error, response) => {
      if (response.status === 200){

        // TODO : C'est le plugin qui donne son nom au store.
        // Le nom renvoyé par le serveur ne sert a rien
        // Gestion de plusieurs instances du plugin ?

        // Add event listenner for plugin to load
        document.addEventListener('plugin', function (plugin) {
          // When plugin loaded event is received, add plugin to the store
          if (callback){
            callback(plugin.detail);
          }
          store.dispatch(addPlugin(plugin.detail.name,plugin.detail.app));
        },false);

          // Check if there is plugins to load
          if (response.body && response.body.length > 0){
            console.log(response.body.length + " plugins to load ...");

            // Load each plugin
            let index=0;
            let error=0;
            response.body.map( plugin => {
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
                  if (index === response.body.length){
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
