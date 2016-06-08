module.exports = {
  path:"/admin/:project",

  getChildRoutes(location, cb) {
    require.ensure([], (require) => {
      cb(null, [
        require('./modules/home/routes'),
        require('./modules/test/routes')
      ])
    })
  },

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./AdminApp'))
    })
  }
}
