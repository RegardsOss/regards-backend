import WebSockets from './WebSockets'

export default {
  path:"time",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: WebSockets
      })
    })
  }
}
