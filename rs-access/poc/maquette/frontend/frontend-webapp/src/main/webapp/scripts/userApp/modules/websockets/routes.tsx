import WebSockets from './WebSockets'

declare var require: any;

export default {
  path:"time",

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, {
        content: WebSockets
      })
    })
  }
}
