/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
import { portalContainer } from "@regardsoss/portal"
import { adminRouter } from "@regardsoss/admin"
import { PlainRoute } from "react-router"

export const routes: PlainRoute = {
  path: "/",
  childRoutes: [
      adminRouter
  ],
  getIndexRoute(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, {
        component: portalContainer
      })
    })
  }
}


// Log sitemap
function getSiteMap(parentRoute: any, childRoutes: Array<PlainRoute>): void {
  childRoutes.map((route) => {
    if (route) {
      let path = ''
      if (parentRoute.slice(-1) === '/' || route.path[0] === '/') {
        path = parentRoute + route.path
      } else {
        path = parentRoute + '/' + route.path
      }
      console.log(path)
      if (route.childRoutes) {
        getSiteMap(path, route.childRoutes)
      }
    }
  })
}
// Log sitemap
getSiteMap("", routes.childRoutes)
