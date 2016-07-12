import { PlainRoute } from 'react-router'

declare var require: any;

export const testRoutes:PlainRoute = {
  path: 'test',

  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: require('./Test')
      })
    })
  }
}
