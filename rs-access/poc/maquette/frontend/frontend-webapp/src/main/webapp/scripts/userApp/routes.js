import pluginRoutes from './modules/plugin/routes'
import testRoutes from './modules/test/routes'
import websocketsRoutes from './modules/websockets/routes'
import UserApp from './UserApp'

export default {
  path:"user/:project",
  childRoutes: [
    pluginRoutes, 
    testRoutes,
    websocketsRoutes
  ],

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, UserApp)
    })
  }
}
