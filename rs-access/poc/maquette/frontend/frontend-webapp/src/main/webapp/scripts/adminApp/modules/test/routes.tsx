declare var module: any;
declare var require: any;

const routes = {
  path: 'test',

  getComponents(nextState: any, cb: any) {
    require.ensure([], (require: any) => {
      cb(null, {
        content: require('./Test')
      })
    })
  }
}

export default routes
module.exports = routes
