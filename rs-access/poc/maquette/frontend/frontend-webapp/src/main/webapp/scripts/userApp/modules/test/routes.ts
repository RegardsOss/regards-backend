import Test from './Test'

declare var require: any;

export default {
  path:"test",

  getComponent(nextState:any, cb:any) {
    require.ensure([], (require:any) => {
      cb(null, {
        content: Test
      })
    })
  }
}
