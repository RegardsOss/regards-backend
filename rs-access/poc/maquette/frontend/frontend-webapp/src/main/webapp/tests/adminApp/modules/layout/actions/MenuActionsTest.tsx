import { expect } from 'chai';
import * as actions from '../../../../../scripts/adminApp/modules/layout/actions/MenuActions';

describe('[ADMIN APP] Testing Menu actions', () => {

  it('should create an action to show projects', () => {
    const expectedAction = {
      type: 'SHOW_PROJECTS'
    }

    expect(actions.showProjects()).to.eql(expectedAction)
  })
})
