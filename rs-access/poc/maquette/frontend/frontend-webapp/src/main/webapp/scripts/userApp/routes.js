module.exports = {
  path:"user/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./modules/plugin/routes'),
        require('./modules/test/routes'),
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./UserApp'))
    })
  }
}
