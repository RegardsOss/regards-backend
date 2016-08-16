import { PlainRoute } from "react-router"
import UsersContainer from './containers/UsersContainer'

import UserEditComponent from './components/UserEditComponent'
import UserCreateComponent from './components/UserCreateComponent'

export const usersRoute: PlainRoute = {
  path: 'users',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: UsersContainer
      })
    })
  }
}

export const userViewRoute: PlainRoute = {
  path: 'users/:user_id',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: UserEditComponent
      })
    })
  }
}

export const userEditRoute: PlainRoute = {
  path: 'users/:user_id/edit',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: UserEditComponent
      })
    })
  }
}

export const userCreateRoute: PlainRoute = {
  path: 'user/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content:UserCreateComponent
      })
    })
  }
}

export const usersRoutes = {
  usersRoute,
  userViewRoute,
  userEditRoute,
  userCreateRoute
}
