import ProjectsContainer from "./containers/ProjectsContainer"
import { PlainRoute } from "react-router"

export const projectsRoutes: PlainRoute = {
  path: 'projects',
  getComponents(nextState: any, cb: any): any {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: ProjectsContainer
      })
    })
  }
}
