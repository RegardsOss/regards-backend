declare var require: any;

const routes = {
  path: 'home',

  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: require('./Home')
      })
    })
  }
}

export default routes
module.exports = routes
