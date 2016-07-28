import MockContainer from './containers/MockContainer'
import { PlainRoute } from 'react-router'

declare var require: any;

export const projectUsersRoutes:PlainRoute = {
  path: 'users',
  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: MockContainer
      })
    })
  }
}
