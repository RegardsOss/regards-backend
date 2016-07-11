import Plugin from './Plugin'

export default {
  path:"plugins/:plugin",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: Plugin
      })
    })
  }
}
