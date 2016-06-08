module.exports = {
  path:"test",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./Test')
      })
    })
  }
}
