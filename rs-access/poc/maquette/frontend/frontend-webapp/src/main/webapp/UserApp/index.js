module.exports = {
  path:"user/:project",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, require('./UserApp'))
    })
  }
}
