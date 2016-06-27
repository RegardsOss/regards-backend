/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
*/
import userAppRoutes from './userApp/routes'
import adminAppRoutes from './adminApp/routes'
import portalAppRoutes from './portalApp/routes'
import PortalApp from './portalApp/PortalApp'

export default {
  component: 'div',
  childRoutes: [ {
    path: '/',
    childRoutes: [
      userAppRoutes,
      adminAppRoutes,
      portalAppRoutes
    ],
    getComponent(nextState, cb) {
      require.ensure([], (require) => {
        cb(null, PortalApp)
      })
    }
  } ]
}
