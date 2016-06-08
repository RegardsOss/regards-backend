module.exports = {
  path:"portal",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./PortalApp'))
    })
  }
}
