import Test from './Test'

export default {
  path:"test",

  getComponent(nextState, cb) {
    require.ensure([], (require) => {
      cb(null, {
        content: Test
      })
    })
  }
}
