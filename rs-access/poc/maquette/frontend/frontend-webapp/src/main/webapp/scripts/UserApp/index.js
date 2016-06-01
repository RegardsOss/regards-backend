module.exports = {
  path:"user/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./PluginModule'),
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./UserApp'))
    })
  }
}
