/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
import { userAppRoutes } from "./userApp/routes";
import { adminAppRoutes } from "./adminApp/routes";
import { portalAppRoutes } from "./portalApp/routes";
import PortalApp from "./portalApp/PortalApp";
import { PlainRoute } from "react-router";

declare var require: any;

const childRoutes: Array<PlainRoute> = [{
  path: "/",
  childRoutes: [
    adminAppRoutes,
    userAppRoutes,
    portalAppRoutes
  ],
  getIndexRoute(nextState: any, cb: any): void {
    require.ensure ([], (require: any) => {
      cb (null, {
        component: PortalApp
      })
    })
  }
}]

export const routes: PlainRoute = {
  childRoutes: childRoutes
}
