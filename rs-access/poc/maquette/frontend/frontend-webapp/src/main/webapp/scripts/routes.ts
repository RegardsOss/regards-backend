/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
import { userAppRoutes } from './userApp/routes'
import { adminAppRoutes } from './adminApp/routes'
import { portalAppRoutes } from './portalApp/routes'
import PortalApp from './portalApp/PortalApp'

import { PlainRoute } from 'react-router'

declare var require: any;

const childRoutes:Array<PlainRoute> = [ {
  path: '/',
  childRoutes: [
    adminAppRoutes,
    userAppRoutes,
    portalAppRoutes
  ],
  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, PortalApp)
    })
  }
} ]

export const routes:PlainRoute = {
  childRoutes: childRoutes
}
