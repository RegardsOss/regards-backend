module.exports = {
  path:"user/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./modules/plugin'),
        require('./modules/test'),
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./UserApp'))
    })
  }
}
