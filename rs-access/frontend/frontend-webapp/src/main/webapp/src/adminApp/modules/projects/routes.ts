import ProjectsContainer from "./containers/ProjectsContainer"
import { PlainRoute } from "react-router"
import { ThemedProjectReadComponent } from "./components/ProjectReadComponent"

export const projectsRoutes: PlainRoute = {
  path: 'projects',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectsContainer
      })
    })
  }
}

export const projectReadRoute: PlainRoute = {
  path: 'projects/:project_id',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ThemedProjectReadComponent
      })
    })
  }
}