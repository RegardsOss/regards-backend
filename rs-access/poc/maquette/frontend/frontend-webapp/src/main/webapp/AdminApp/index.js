module.exports = {
  path:"/admin/:project",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./AdminApp'))
    })
  }
}
