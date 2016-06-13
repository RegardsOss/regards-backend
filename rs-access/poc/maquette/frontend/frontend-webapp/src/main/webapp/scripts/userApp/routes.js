module.exports = {
  path:"user/:project",
  childRoutes: [
    require('./modules/plugin/routes'),
    require('./modules/test/routes'),
    require('./modules/websockets/routes'),
  ],

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./UserApp'))
    })
  }
}
