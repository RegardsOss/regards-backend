import PortalApp from './PortalApp'

export default {
  path:"portal",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, PortalApp)
    })
  }
}
