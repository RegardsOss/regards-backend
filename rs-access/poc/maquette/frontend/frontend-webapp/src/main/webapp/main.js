import React from 'react'
import { Router, browserHistory } from 'react-router';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import store from 'AppStore';
import { loadViewsAccessRights } from 'Common/ModulesManager/RegardsModulesControler';

const rootRoute = {
  component: 'div',
  childRoutes: [ {
    path: '/',
    component: require('./PortalApp/PortalApp'),
    childRoutes: [
      require('./UserApp'),
      require('./AdminApp')
    ]
  } ]
}

// Load views access rights
if (store.getState().viewsLoaded === false){
  loadViewsAccessRights();
}

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory} routes={rootRoute}/>
  </Provider>,
  document.getElementById('app')
);
