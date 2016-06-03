module.exports = {
  path:"plugins/:plugin",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: require('./PluginModule')
      })
    })
  }
}
