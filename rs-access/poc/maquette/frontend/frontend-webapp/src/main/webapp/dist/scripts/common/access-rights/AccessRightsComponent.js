"use strict";
const React = require("react");
const AccessRightsActions_1 = require("./AccessRightsActions");
class AccessRightsComponent extends React.Component {
    constructor() {
        super();
        this.unsubscribeViewAccessRights = null;
        this.state = {
            access: false
        };
        this.checkViewAccessRights = this.checkViewAccessRights.bind(this);
    }
    getDependencies() {
        return null;
    }
    componentWillMount() {
        const { store } = this.context;
        if (this.getDependencies() === null) {
            this.setState({
                access: true
            });
        }
        else {
            this.unsubscribeViewAccessRights = store.subscribe(this.checkViewAccessRights);
            store.dispatch(AccessRightsActions_1.fetchAccessRights(this.constructor.name, this.getDependencies()));
        }
    }
    componentWillUnmount() {
        if (this.unsubscribeViewAccessRights) {
            this.unsubscribeViewAccessRights();
        }
    }
    checkViewAccessRights() {
        const { store } = this.context;
        const view = store.getState().views.find((curent) => {
            return curent.name === this.constructor.name;
        });
        if (view) {
            if (view.access === true) {
                console.log("Access granted to view : " + this.constructor.name);
            }
            else {
                console.log("Access denied to view : " + this.constructor.name);
                this.render = () => { return null; };
            }
            this.unsubscribeViewAccessRights();
            this.setState({
                access: view.access
            });
        }
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = AccessRightsComponent;
//# sourceMappingURL=AccessRightsComponent.js.map