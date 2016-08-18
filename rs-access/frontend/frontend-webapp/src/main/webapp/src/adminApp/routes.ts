import { PlainRoute } from "react-router"

import { projectsRoutes } from "./modules/projects/routes"
import { projectAccountsRoutes, projectAccountEditRoute, projectAccountCreateRoute } from "./modules/userManagement/routes"
import { datamanagementRoute, datasetCreateRoute, datasetRoute } from "./modules/datamanagement/routes"
import AdminApp from "./AdminApp"



export const adminAppRoutes: PlainRoute = {
  path: "admin/:project",
  childRoutes: [
    projectsRoutes,
    projectAccountEditRoute,
    projectAccountsRoutes,
    projectAccountCreateRoute,
    datamanagementRoute,
    datasetRoute,
    datasetCreateRoute
  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure ([], (require: any) => {
      cb (null, AdminApp)
    })
  }
}
