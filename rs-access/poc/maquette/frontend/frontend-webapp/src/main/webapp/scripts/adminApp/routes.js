import homeRoutes from './modules/home/routes'
import testRoutes from './modules/test/routes'
import projectsRoutes from './modules/projects/routes'
import AdminApp from './AdminApp'

export default {
  path:"admin/:project",

  childRoutes: [
    homeRoutes, testRoutes, projectsRoutes
  ],

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, AdminApp)
    })
  }
}
