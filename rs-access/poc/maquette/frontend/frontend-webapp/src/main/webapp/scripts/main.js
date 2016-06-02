import React from 'react'
import ReactDOM from 'react-dom';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';
import { Rest } from 'grommet';

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

const init = (err, response) => {
  // Add token to rest requests
  Rest.setHeaders({
    'Authorization': "Bearer " + response.body.access_token
  });

  // Render application
  ReactDOM.render(
    <Provider store={store}>
      <Router history={browserHistory} routes={rootRoute}/>
    </Provider>,
    document.getElementById('app')
  );
}

// First login as public user
Rest.setHeaders({
  'Accept': 'application/json',
  'Authorization': "Basic " + btoa("acme:acmesecret")
});
const userName = "public";
const password = "public";
const location = "http://localhost:8080/oauth/token?grant_type=password&username="
+ userName + "&password=" +password;
Rest.post(location).end(init);
