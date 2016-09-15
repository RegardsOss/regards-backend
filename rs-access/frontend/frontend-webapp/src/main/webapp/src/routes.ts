/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
import { adminRouter } from "@regardsoss/admin"
/*
import { userAppRoutes } from "./userApp/routes"
import { portalAppRoutes } from "./portalApp/routes"
import PortalApp from "./portalApp/PortalApp"
*/
import { PlainRoute } from "react-router"

const childRoutes: Array<PlainRoute> = [{
  path: "/",
  childRoutes: [
    adminRouter/*,
    userAppRoutes,
    portalAppRoutes*/
  ],
  getIndexRoute(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, {
        // component: PortalApp
        component: adminRouter
      })
    })
  }
}]

export const routes: PlainRoute = {
  childRoutes: childRoutes
}
