import { PlainRoute } from "react-router"
import { dataManagementRouter } from "@regardsoss/admin-data-management"

export const adminRouter: Array<PlainRoute> = [{
  path: "/",
  childRoutes: [
    dataManagementRouter
  ],
  getIndexRoute(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, {
        // component: PortalApp
        component: dataManagementRouter
      })
    })
  }
}]
