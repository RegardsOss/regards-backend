import React from 'react'
import ReactDOM from 'react-dom';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';
import Rest from 'grommet/utils/Rest';

// Import application common store
import store from 'AppStore';

/** Main routes.
 * / -> PortalApp
 * /user -> UserApp
 * /admin -> AdminApp
*/
const rootRoute = {
  component: 'div',
  childRoutes: [ {
    path: '/',
    component: require('./portalApp/PortalApp'),
    childRoutes: [
      require('./userApp'),
      require('./adminApp')
    ]
  } ]
}

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory} routes={rootRoute}/>
  </Provider>,
  document.getElementById('app')
);
