module.exports = {
  path: './',

  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./Home')
      })
    })
  }
}
