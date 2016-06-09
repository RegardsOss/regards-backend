/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
*/
module.exports = {
  component: 'div',
  childRoutes: [ {
    path: '/',
    childRoutes: [
      require('./userApp/routes'),
      require('./adminApp/routes'),
      require('./portalApp/routes')
    ],
    getComponent(nextState, cb) {
      require.ensure([], (require) => {
        cb(null, require('./portalApp/PortalApp'))
      })
    }
  } ]
}
