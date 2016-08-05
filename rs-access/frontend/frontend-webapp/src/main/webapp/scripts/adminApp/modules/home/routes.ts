import { PlainRoute } from "react-router"

export const homeRoutes: PlainRoute = {
  path: 'home',

  getComponents(nextState: any, cb: any): any {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: require ('./Home')
      })
    })
  }
}
