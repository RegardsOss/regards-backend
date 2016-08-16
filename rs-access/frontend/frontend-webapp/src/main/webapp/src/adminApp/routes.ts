import { homeRoutes } from "./modules/home/routes"
import { projectsRoutes } from "./modules/projects/routes"
import { projectUsersRoutes, projectUserEditRoute, projectUserCreateRoute } from "./modules/projectUsers/routes"
import AdminApp from "./AdminApp"
import { PlainRoute } from "react-router"
import { datamanagementRoute, datasetCreateRoute } from "./modules/datamanagement/routes"

export const adminAppRoutes: PlainRoute = {
  path: "admin/:project",
  childRoutes: [
    homeRoutes,
    projectsRoutes,
    projectUserEditRoute,
    projectUsersRoutes,
    projectUserCreateRoute,
    datamanagementRoute,
    datasetCreateRoute
  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure ([], (require: any) => {
      cb (null, AdminApp)
    })
  }
}
