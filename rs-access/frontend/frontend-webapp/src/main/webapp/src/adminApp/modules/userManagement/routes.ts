import ProjectAcountsContainer from "./containers/ProjectAccountsContainer"
import ProjectAccountEditContainer from "./containers/ProjectAccountEditContainer"
import { PlainRoute } from "react-router"
import ProjectAccountCreateContainer from "./containers/ProjectAccountCreateContainer"

export const projectAccountCreateRoute: PlainRoute = {
  path: 'users/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectAccountCreateContainer
      })
    })
  }
}

export const projectAccountEditRoute: PlainRoute = {
  path: 'users/:user_id',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectAccountEditContainer
      })
    })
  }
}

export const projectAccountsRoutes: PlainRoute = {
  path: 'users',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ProjectAcountsContainer
      })
    })
  }
}
