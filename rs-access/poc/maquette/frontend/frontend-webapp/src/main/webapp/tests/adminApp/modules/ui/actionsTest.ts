import { expect } from 'chai' // You can use any testing library
import * as actions from '../../../../scripts/adminApp/modules/ui/actions';

describe('[ADMIN APP] Testing ui actions', () => {

  it('should create an action to show project configuration', () => {
    const expectedAction = {
      type: 'SHOW_PROJECT_CONFIGURATION'
    }
    expect(actions.showProjectConfiguration()).to.eql(expectedAction)
  })

  it('should create an action to hide project configuration', () => {
    const expectedAction = {
      type: 'HIDE_PROJECT_CONFIGURATION'
    }
    expect(actions.hideProjectConfiguration()).to.eql(expectedAction)
  })

  it('should create an action to select a project', () => {
    const expectedAction = {
      type: 'SELECT_PROJECT',
      id: 'toto'
    }
    expect(actions.selectProject('toto')).to.eql(expectedAction)
  })

  it('should create an action to select a project admin', () => {
    const expectedAction = {
      type: 'SELECT_PROJECT_ADMIN',
      id: 'toto'
    }
    expect(actions.selectProjectAdmin('toto')).to.eql(expectedAction)
  })

  it('should create an action to show project admin configuration', () => {
    const expectedAction = {
      type: 'SHOW_PROJECT_ADMIN_CONFIGURATION'
    }
    expect(actions.showProjectAdminConfiguration()).to.eql(expectedAction)
  })

  it('should create an action to hide project admin configuration', () => {
    const expectedAction = {
      type: 'HIDE_PROJECT_ADMIN_CONFIGURATION'
    }
    expect(actions.hideProjectAdminConfiguration()).to.eql(expectedAction)
  })

})
