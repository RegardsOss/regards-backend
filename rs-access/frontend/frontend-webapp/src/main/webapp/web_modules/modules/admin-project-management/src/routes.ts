import ProjectsContainer from "./containers/ProjectsContainer"
import { PlainRoute } from "react-router"
import ProjectReadContainer from "./containers/ProjectReadContainer"
import ProjectCreateContainer from "./containers/ProjectCreateContainer"

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
        content: ProjectReadContainer
      })
    })
  }
}

export const projectCreateRoute: PlainRoute = {
  path: 'projects/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectCreateContainer
      })
    })
  }
}