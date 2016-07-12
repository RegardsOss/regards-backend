/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
import userAppRoutes from './userApp/routes'
import adminAppRoutes from './adminApp/routes'
import portalAppRoutes from './portalApp/routes'
import PortalApp from './portalApp/PortalApp'

declare var require: any;

export const routes:any = {
  component: 'div',
  childRoutes: [ {
    path: '/',
    childRoutes: [
      userAppRoutes,
      adminAppRoutes,
      portalAppRoutes
    ],
    getComponent(nextState:any, cb:any) {
      require.ensure([], (require:any) => {
        cb(null, PortalApp)
      })
    }
  } ]
}
