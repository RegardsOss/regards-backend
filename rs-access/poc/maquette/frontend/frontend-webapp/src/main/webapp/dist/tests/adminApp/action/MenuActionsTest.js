"use strict";
const chai_1 = require('chai');
const MenuActions_1 = require('../../../scripts/adminApp/modules/layout/actions/MenuActions');
describe('Testing Menu actions', () => {
    describe('Testing showProjects action', () => {
        it('Should be a function', () => {
            chai_1.expect(MenuActions_1.showProjects).to.be.a('function');
        });
        it('Should return the object { type : "SHOW_PROJECTS" }', () => {
            chai_1.expect(MenuActions_1.showProjects()).to.deep.equal({ type: "SHOW_PROJECTS" });
        });
    });
});
//# sourceMappingURL=MenuActionsTest.js.map