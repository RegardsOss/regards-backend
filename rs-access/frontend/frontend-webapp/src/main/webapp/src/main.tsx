import * as ReactDOM from "react-dom"
import { Router, browserHistory, PlainRoute } from "react-router"
import { Provider } from "react-redux"
import configureStore from "./common/store/configureStore"
import preloadedState from "./common/store/preloadedState"
import { routes } from "./routes"
import {test} from "@regards-oss/common"


console.log(test())

const store = configureStore (preloadedState)
ReactDOM.render (
  <Provider store={store}>
    <Router history={browserHistory} routes={routes}/>
  </Provider>,
  document.getElementById ('app')
)

// Log sitemap
function getSiteMap(parentRoute: any, routes: Array<PlainRoute>): void {
  routes.map ((route) => {
    if (route) {
      let path = ''
      if (parentRoute.slice (-1) === '/' || route.path[0] === '/') {
        path = parentRoute + route.path
      } else {
        path = parentRoute + '/' + route.path
      }
      console.log (path)
      if (route.childRoutes) {
        getSiteMap (path, route.childRoutes)
      }
    }
  })
}
// Log sitemap
getSiteMap ("", routes.childRoutes)
