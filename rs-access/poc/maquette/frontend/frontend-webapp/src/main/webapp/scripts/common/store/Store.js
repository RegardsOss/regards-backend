import thunkMiddleware from 'redux-thunk'
import { createStore, combineReducers, applyMiddleware } from 'redux'

import loggerMiddleware from 'common/logger/ActionLoggerMiddleware'

import themeReducers from 'common/theme/reducers/ThemeReducers'
import pluginReducers from 'common/plugins/PluginReducers'
import accessRightsReducers from 'common/access-rights/AccessRightsReducers'
import authenticateReducers from 'common/authentication/AuthenticateReducers'
import projectsReducers from 'portalApp/modules/projects/reducers/ProjectsReducers'
import timeReducers from 'userApp/modules/websockets/reducers/TimeReducers'
import adminAppLayoutReducers from 'adminApp/modules/layout/reducers/index.js'
import adminAppProjectsReducers from 'adminApp/modules/projects/reducers/ProjectsReducers'
// import { formReducer } from 'redux-form';
// import adminAppReducers from 'adminApp/reducers'

// Create the compined reducers by adding all modules reducers
const allReducers = Object.assign({}, themeReducers,
  pluginReducers, accessRightsReducers, projectsReducers,
  authenticateReducers, timeReducers, adminAppLayoutReducers, adminAppProjectsReducers)
const reducers = combineReducers(allReducers)

// Default store values
const defaultStore = {
  theme: '',
  plugins : {},
  views : [],
  authentication : {},
  projects : {
    items: [
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
  },
  ws: {}
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
