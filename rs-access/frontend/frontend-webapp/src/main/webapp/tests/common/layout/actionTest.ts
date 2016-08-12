import { expect } from 'chai';
import * as actions from '../../../src/common/layout/actions'

describe('[COMMON] Testing layout actions', () => {

  it('should create an action to set the layout', () => {
    const expectedAction = {
      type: 'SET_LAYOUT',
      layout: 'super-cool-layout'
    }
    expect(actions.setLayout('super-cool-layout')).to.eql(expectedAction)
  })
})
