import Plugin from "./Plugin";
import { PlainRoute } from "react-router";

declare var require: any;

export const pluginRoutes: PlainRoute = {
  path: "plugins/:plugin",

  getComponent(nextState: any, cb: any) {
    require.ensure ([], (require: any) => {
      cb (null, {
        content: Plugin
      })
    })
  }
}
