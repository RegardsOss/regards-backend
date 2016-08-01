import MockContainer from "./containers/MockContainer";
import ProjectUserEditContainer from "./containers/ProjectUserEditContainer";
import { PlainRoute } from "react-router";

declare var require: any;

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
        content: MockContainer
      })
    })
  }
}
