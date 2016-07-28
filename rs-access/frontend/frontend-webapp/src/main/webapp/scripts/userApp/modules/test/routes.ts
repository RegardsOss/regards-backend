import TestContainer from './TestContainer'
import { PlainRoute } from 'react-router'

declare var require: any;

export const testRoutes:PlainRoute = {
  path:"test",
  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, {
        content: TestContainer
      })
    })
  }
}
