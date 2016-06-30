import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware, compose } from 'redux'
import loggerMiddleware from 'common/logger/ActionLoggerMiddleware'
import authorizationMiddleware from 'common/authentication/AuthorizationMiddleware'
import { apiMiddleware } from 'redux-api-middleware';
// Root reducers
import adminApp from 'adminApp/reducer'
import userApp from 'userApp/reducers'
import portalApp from 'portalApp/reducers'
import common from 'common/reducers'
import {reducer as formReducer} from 'redux-form';

export default function configureStore(preloadedState) {
  // Root reducer
  const rootReducer = combineReducers({
    userApp,
    portalApp,
    common,
    adminApp,
    form: formReducer
  })

  // Create the application store
  const store = createStore(
    rootReducer,
    preloadedState,
    compose(
      applyMiddleware(
        thunkMiddleware, // lets us dispatch() functions
        loggerMiddleware, // logs any dispatched action
        authorizationMiddleware, // inject authorization headers in all request actions
        apiMiddleware // middleware for calling an REST API
      ),
      window.devToolsExtension ? window.devToolsExtension() : f => f // Enable redux dev tools
    )
  )

  // Log any change in the store
  const render = () => {
    console.log("STORE UPDATED : ",store.getState())
  }
  store.subscribe(render)

  // Enable Webpack hot module replacement for reducers
  if (module.hot) {
    module.hot.accept('../reducers', () => {
      const nextRootReducer = require('../reducers').default
      store.replaceReducer(nextRootReducer)
    })
  }

  return store
}
