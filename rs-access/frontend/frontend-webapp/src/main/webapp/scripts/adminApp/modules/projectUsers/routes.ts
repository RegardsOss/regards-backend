import ProjectUsersContainer from "./containers/ProjectUsersContainer";
import ProjectUserEditContainer from "./containers/ProjectUserEditContainer";
import { PlainRoute } from "react-router";
import ProjectUserCreateContainer from "./containers/ProjectUserCreateContainer";

export const projectUserCreateRoute: PlainRoute = {
  path: 'users/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: ProjectUserCreateContainer
      })
    })
  }
}

export const projectUserEditRoute: PlainRoute = {
  path: 'users/:user_id',
  getComponents(nextState: any, cb: any): any {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: ProjectUserEditContainer
      })
    })
  }
}

export const projectUsersRoutes: PlainRoute = {
  path: 'users',
  getComponents(nextState: any, cb: any): any {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: ProjectUsersContainer
      })
    })
  }
}
