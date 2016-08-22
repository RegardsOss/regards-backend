import { expect } from 'chai'
import reducer from '../reducer'

describe('[COMMON] Testing layout reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({})
  })

  it('should set the layout', () => {
    const action = {
      type: 'SET_LAYOUT',
      layout: {lg:'super-cool-layout'}
    }
    let initState = {}
    const expectedState = {lg:'super-cool-layout'}
    expect(reducer(initState, action)).to.eql(expectedState)

    initState = {lg:'shitty-layout'}
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
