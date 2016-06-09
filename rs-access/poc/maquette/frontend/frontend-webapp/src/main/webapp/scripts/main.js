import React from 'react'
import ReactDOM from 'react-dom';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';

// Import application common store
import store from 'AppStore';
import routes from './routes.js';

// Import fundation classes
//import 'stylesheets/vendors/vendors.scss';

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory} routes={routes}/>
  </Provider>,
  document.getElementById('app')
);

// Log sitemap
function getSiteMap(parentRoute, routes){
  routes.map( route => {
    let path = ''
    if (parentRoute.slice(-1) === '/' || route.path[0] === '/'){
      path = parentRoute + route.path;
    } else {
      path = parentRoute + '/' + route.path;
    }
    console.log(path);
    if (route.childRoutes){
      getSiteMap(path, route.childRoutes);
    }
  });
}
// Log sitemap
getSiteMap("",routes.childRoutes);
