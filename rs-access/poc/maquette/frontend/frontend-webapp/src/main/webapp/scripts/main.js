import React from 'react'
import ReactDOM from 'react-dom';
import { Router, browserHistory } from 'react-router';
import { Provider } from 'react-redux';
import { Rest } from 'grommet';

import ApplicationError from 'common/components/ApplicationErrorComponent';

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


// First login as public user
Rest.setHeaders({
  'Accept': 'application/json',
  'Authorization': "Basic " + btoa("acme:acmesecret")
});
const userName = "public";
const password = "public";
const location = "http://localhost:8080/oauth/token?grant_type=password&username="
+ userName + "&password=" +password;

// Send rest request for authentication. After response, render the application
Rest.post(location).end((err, response) => {
  if (response && response.status === 200){
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
  else {
    console.error("Authentication error");
    ReactDOM.render(
      <Provider store={store}>
        <ApplicationError theme=""/>
      </Provider>, document.getElementById('app'));
  }
});
