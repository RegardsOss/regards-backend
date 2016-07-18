import { expect } from 'chai' // You can use any testing library
import * as actions from '../../../../scripts/common/theme/actions/ThemeActions'

describe('[COMMON] Testing theme actions', () => {

  it('should create an action to set the theme', () => {
    const expectedAction = {
      type: 'SET_THEME',
      theme: 'toto'
    }
    expect(actions.setTheme('toto')).to.eql(expectedAction)
  })

})
