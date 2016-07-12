import ProjectsContainer from './containers/ProjectsContainer'
import { PlainRoute } from 'react-router'

declare var require: any;

export const projectsRoutes:PlainRoute = {
  path: 'projects',
  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectsContainer
      })
    })
  }
}
