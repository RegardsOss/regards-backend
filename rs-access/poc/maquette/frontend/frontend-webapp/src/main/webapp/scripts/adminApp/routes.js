module.exports = {
  path:"admin/:project",

  childRoutes: [
    require('./modules/home/routes'),
    require('./modules/test/routes'),
    require('./modules/projects/routes')
  ],

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./AdminApp'))
    })
  }
}
