module.exports = {
  path:"/admin/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./modules/HomeModule'),
        require('./modules/TestModule')
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./AdminApp'))
    })
  }
}
