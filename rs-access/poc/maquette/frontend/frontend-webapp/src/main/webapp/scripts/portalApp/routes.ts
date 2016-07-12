import PortalApp from './PortalApp'

declare var require: any;

export default {
  path:"portal",

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, PortalApp)
    })
  }
}
