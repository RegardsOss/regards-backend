import { PlainRoute } from "react-router";

declare var require: any;

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
