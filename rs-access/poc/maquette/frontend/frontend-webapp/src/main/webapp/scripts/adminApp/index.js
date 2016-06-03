module.exports = {
  path:"/admin/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./modules/home'),
        require('./modules/test')
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./AdminApp'))
    })
  }
}
