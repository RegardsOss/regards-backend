
import { Rest } from 'grommet';

import { addViewAccess, viewsLoaded } from './RegardsModuleActionCreators';
import store from 'AppStore';

const loadViewsAccessRights = () => {
  // Get plugins from server
  const location = window.location.origin + '/json/viewsAccessRights.json';
  return Rest.get(location)
    .end((error, response) => {
      if (response.status === 200){
          // Check if there is plugins to load
          if (response.body.views && response.body.views.length > 0){
            response.body.views.map( view => {
              if (view) {
                if (view.access === true){
                  store.dispatch(addViewAccess(view.name));
                }
              }
            });
          }
      }
      store.dispatch(viewsLoaded());
    });
}


export { loadViewsAccessRights }
