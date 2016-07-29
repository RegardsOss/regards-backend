import { pluginRoutes } from "./modules/plugin/routes";
import { testRoutes } from "./modules/test/routes";
import { websocketsRoutes } from "./modules/websockets/routes";
import UserApp from "./UserApp";
import { PlainRoute } from "react-router";

declare var require: any;

export const userAppRoutes: PlainRoute = {
    path: 'user/:project',
    childRoutes: [
        pluginRoutes,
        testRoutes,
        websocketsRoutes
    ],
    getComponent(nextState: any, cb: any): void {
        require.ensure ([], (require: any) => {
            cb (null, UserApp)
        })
    }
}
