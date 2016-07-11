"use strict";
const React = require('react');
const AccessRightsComponent_1 = require('../../../common/access-rights/AccessRightsComponent');
class Test extends AccessRightsComponent_1.default {
    getDependencies() {
        return {
            'GET': ["dependencies"]
        };
    }
    render() {
        return (React.createElement("div", null, "This view shall not be displayed ! "));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = Test;
module.exports = Test;
//# sourceMappingURL=Test.js.map