import { homeRoutes } from "./modules/home/routes"
import { projectsRoutes } from "./modules/projects/routes"
import { usersRoute, userViewRoute, userEditRoute, userCreateRoute } from "./modules/userManagement/routes"
import { projectUsersRoutes } from "./modules/projectUsers/routes"
import AdminApp from "./AdminApp"
import { PlainRoute } from "react-router"

export const adminAppRoutes: PlainRoute = {
  path: "admin/:project",
  childRoutes: [
    homeRoutes,
    projectsRoutes,
    usersRoute,
    userViewRoute,
    userEditRoute,
    userCreateRoute,
    projectUsersRoutes

  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure ([], (require: any) => {
      cb (null, AdminApp)
    })
  }
}
