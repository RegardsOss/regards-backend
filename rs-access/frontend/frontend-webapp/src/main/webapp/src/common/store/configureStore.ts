import { createStore, applyMiddleware, compose } from "redux"
import thunk from "redux-thunk"
import authorizationMiddleware from "../authentication/AuthorizationMiddleware"
import * as createLogger from "redux-logger"
import rootReducer from "../../reducer"
// Middlewares
let {apiMiddleware} = require ('redux-api-middleware')
// Root reducer

export default function configureStore(preloadedState: any): any {
  const logger = createLogger () // Pass an options object for specific configuration

  // Define the used middlewares (order matters)
  const middlewares = [
    thunk, // lets us dispatch() functions
    authorizationMiddleware, // inject authorization headers in all request actions
    apiMiddleware, // middleware for calling an REST API
    logger // Logger must be the last middleware in chain, otherwise it will log thunk and promise, not actual actions
  ]

  // Create the application store
  let win: any = window
  const store = createStore (
    rootReducer,
    preloadedState,
    compose (
      applyMiddleware (...middlewares),
      win.devToolsExtension ? win.devToolsExtension () : (f: any) => f // Enable redux dev tools
    )
  )

  return store
}
