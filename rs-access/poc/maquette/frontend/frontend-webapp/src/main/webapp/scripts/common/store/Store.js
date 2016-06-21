import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware } from 'redux'
import loggerMiddleware from 'common/logger/ActionLoggerMiddleware'
// Root reducers
import adminApp from 'adminApp/reducers'
import userApp from 'userApp/reducers'
import portalApp from 'portalApp/reducers'
import common from 'common/reducers'
import {reducer as formReducer} from 'redux-form';

const reducers = combineReducers({
  userApp,
  portalApp,
  common,
  adminApp,
  form: formReducer
})

// Default store values
const defaultStore = {
  common: {
    theme: '',
    plugins : {},
    views : [],
    authentication : {}
  },
  userApp : {
    ws : {}
  },
  portalApp : {
    projects : {}
  },
  adminApp : {
    projects : {
      items : [
        {
          id: "0",
          name: 'Project X',
          selected: false,
          admins: [
            {
              id: 'toto',
              name: 'Toto'
            },
            {
              id: 'titi',
              name: 'Titi'
            }
          ]
        },
        {
          id: "1",
          name: 'Blair witch project',
          selected: false,
          admins: [
            {
              id: 'momo',
              name: 'Momo'
            },
            {
              id: 'mimi',
              name: 'Mimi'
            }
          ]
        }
      ]
    }
  }
}
// Create the application store
const store = createStore(
  reducers,
  defaultStore,
  applyMiddleware(
    thunkMiddleware, // lets us dispatch() functions
    loggerMiddleware // logs any dispatched action
  )
)

// Log any change in the store
const render = () => {
  console.log("STORE UPDATED : ",store.getState())
}
store.subscribe(render)

export default store
