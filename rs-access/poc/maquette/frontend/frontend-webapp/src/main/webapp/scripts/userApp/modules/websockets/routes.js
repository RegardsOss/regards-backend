module.exports = {
  path:"time",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./WebSockets')
      })
    })
  }
}
