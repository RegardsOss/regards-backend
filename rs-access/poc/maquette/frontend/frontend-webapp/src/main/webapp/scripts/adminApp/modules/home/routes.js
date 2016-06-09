module.exports = {
  path: 'home',

  getComponents(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./Home')
      })
    })
  }
}
