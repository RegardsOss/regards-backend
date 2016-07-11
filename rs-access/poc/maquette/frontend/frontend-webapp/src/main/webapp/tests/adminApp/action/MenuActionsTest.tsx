import { expect } from 'chai';
import { showProjects } from '../../../scripts/adminApp/modules/layout/actions/MenuActions';

describe('Testing Menu actions', () => {

  describe('Testing showProjects action', () => {
    it('Should be a function', () => {
      expect(showProjects).to.be.a('function')
    })
    it('Should return the object { type : "SHOW_PROJECTS" }', () => {
      expect(showProjects()).to.deep.equal({ type : "SHOW_PROJECTS" })
    })

  })
})
