/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
*/
module.exports = {
  component: 'div',
  childRoutes: [ {
    path: '/',
    component: require('./portalApp/PortalApp'),
    childRoutes: [
      require('./userApp'),
      require('./adminApp')
    ]
  } ]
}
