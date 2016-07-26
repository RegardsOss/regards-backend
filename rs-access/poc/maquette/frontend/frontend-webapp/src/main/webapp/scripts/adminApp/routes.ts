import { homeRoutes } from './modules/home/routes'
import { projectsRoutes } from './modules/projects/routes'
import AdminApp from './AdminApp'

import { PlainRoute } from 'react-router'

declare var require: any;

export const adminAppRoutes:PlainRoute = {
  path:"admin/:project",
  childRoutes: [
    homeRoutes,
    projectsRoutes
  ],
  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, AdminApp)
    })
  }
}
