import React from 'react'
import ReactDOM from 'react-dom';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';

import store from 'AppStore';

// Import default styles
import "common/common";

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

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory} routes={rootRoute}/>
  </Provider>,
  document.getElementById('app')
);
