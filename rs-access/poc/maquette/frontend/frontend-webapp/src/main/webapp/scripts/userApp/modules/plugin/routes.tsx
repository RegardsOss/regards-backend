import Plugin from './Plugin'
declare var require: any;

export default {
  path:"plugins/:plugin",

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, {
        content: Plugin
      })
    })
  }
}
