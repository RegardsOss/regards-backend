import pluginRoutes from './modules/plugin/routes'
import testRoutes from './modules/test/routes'
import websocketsRoutes from './modules/websockets/routes'
import UserApp from './UserApp'

declare var require: any;

export default {
  path:"user/:project",
  childRoutes: [
    pluginRoutes,
    testRoutes,
    websocketsRoutes
  ],

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, UserApp)
    })
  }
}
