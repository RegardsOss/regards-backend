import * as React from 'react'
import * as ReactDOM from 'react-dom'
import { Router, browserHistory, PlainRoute } from 'react-router'
import { Provider } from 'react-redux'

// Import application common store
import configureStore from './common/store/configureStore'
import preloadedState from './common/store/preloadedState'
import { routes } from './routes'

import I18nProvider from './common/i18n/I18nProvider'

const store = configureStore(preloadedState)

ReactDOM.render(
    <Provider store={store}>
      <I18nProvider messageDir='common/i18n/messages'>
        <Router history={browserHistory} routes={routes}/>
      </I18nProvider>
    </Provider>,
  document.getElementById('app')
)

// Log sitemap
function getSiteMap(parentRoute:any, routes:Array<PlainRoute>){
  routes.map( (route) => {
    if (route){
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
    }
  })
}
// Log sitemap
getSiteMap("",routes.childRoutes)
