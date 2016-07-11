import homeRoutes from './modules/home/routes'
import testRoutes from './modules/test/routes'
import projectsRoutes from './modules/projects/routes'
import AdminApp from './AdminApp'

declare var require: any;

export default {
  path:"admin/:project",

  childRoutes: [
    homeRoutes, testRoutes, projectsRoutes
  ],

  getComponent(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, AdminApp)
    })
  }
}
