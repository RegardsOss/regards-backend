import * as React from 'react'
import * as ReactDOM from 'react-dom'
import { Router, browserHistory } from 'react-router'
import { Provider } from 'react-redux'

// Import application common store
import configureStore from './common/store/configureStore'
import preloadedState from './common/store/preloadedState'
import * as routes from './routes'

const store = configureStore(preloadedState)

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory} routes={routes}/>
  </Provider>,
  document.getElementById('app')
)

// Log sitemap
function getSiteMap(parentRoute:any, routes:any){
  routes.map( (route:any) => {
    let path = ''
    if (parentRoute.slice(-1) === '/' || route.path[0] === '/'){
      path = parentRoute + route.path
    } else {
      path = parentRoute + '/' + route.path
    }
    console.log(path)
    if (route.childRoutes){
      getSiteMap(path, route.childRoutes)
    }
  })
}
// Log sitemap
getSiteMap("",routes["childRoutes"])
