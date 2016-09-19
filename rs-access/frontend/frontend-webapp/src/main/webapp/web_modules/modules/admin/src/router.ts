import { PlainRoute } from "react-router"
import { dataManagementRouter } from "@regardsoss/admin-data-management"
import AdminApp from "./AdminApp"
import ProjectAdminApp from "./ProjectAdminApp"


export const projectAdminRouter: PlainRoute = {
  path: ":project",
  childRoutes: [
    dataManagementRouter
  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, ProjectAdminApp)
    })
  }
}



export const adminRouter: PlainRoute = {
  path: "admin",
  childRoutes: [
    projectAdminRouter
  ],
  getComponent(nextState: any, cb: any): void {
    require.ensure([], (require: any) => {
      cb(null, AdminApp)
    })
  }
}
