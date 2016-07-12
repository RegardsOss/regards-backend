import PortalApp from './PortalApp'

import { PlainRoute } from 'react-router'

declare var require: any;

export const portalAppRoutes:PlainRoute = {
  path:"portal",

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, PortalApp)
    })
  }
}
