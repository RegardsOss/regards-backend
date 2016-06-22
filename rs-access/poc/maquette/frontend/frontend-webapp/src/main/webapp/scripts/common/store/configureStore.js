import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware, compose } from 'redux'
import loggerMiddleware from 'common/logger/ActionLoggerMiddleware'
// Root reducers
import adminApp from 'adminApp/reducers'
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
        loggerMiddleware // logs any dispatched action
      ),
      window.devToolsExtension ? window.devToolsExtension() : f => f // Enable redux dev tools
    )
  )

  // Log any change in the store
  const render = () => {
    console.log("STORE UPDATED : ",store.getState())
  }
  store.subscribe(render)

  return store
}
