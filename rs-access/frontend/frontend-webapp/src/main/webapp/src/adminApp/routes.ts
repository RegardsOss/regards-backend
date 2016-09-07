import { PlainRoute } from "react-router"
import { projectsRoutes, projectReadRoute } from "./modules/projects/routes"
import {
  projectAccountsRoutes,
  projectAccountCreateRoute,
  projectAccountReadRoute,
  projectAccountEditRoute
} from "./modules/userManagement/routes"
import { datamanagementRouter } from "./modules/datamanagement/routes"
import AdminApp from "./AdminApp"


export const adminAppRoutes: PlainRoute = {
  path: "admin/:project",
  childRoutes: [
    projectsRoutes,
    projectReadRoute,
    projectAccountsRoutes,
    projectAccountCreateRoute,
    projectAccountReadRoute,
    projectAccountEditRoute,
    datamanagementRouter
  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, AdminApp)
    })
  }
}
