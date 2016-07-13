import { expect } from 'chai'
import { Action } from 'redux'
import reducer from '../../../scripts/common/access-rights/AccessRightsReducers'

describe('[COMMON] Testing access-rigths reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({
      isFetching: false,
      items: []
    })
  })

  it('should handle fetch request', () => {
    const action: Action = {
      type: 'REQUEST_ACCESSRIGHTS'
    }
    const initState = {
      isFetching : false
    }
    const expectedState = {
      isFetching : true
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle fetch success by adding the recceived element', () => {
    const action = {
      type: 'RECEIVE_ACCESSRIGHTS',
      view: 'titi',
      access: false
    }
    const initState = {
      isFetching : true,
      items: [
        {name:'toto', access:true}
      ]
    }
    const expectedState = {
      isFetching : false,
      items: [
        {name:'toto', access:true},
        {name:'titi', access:false}
      ]
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle fetch failure', () => {
    const action = {
      type: 'FAILED_ACCESSRIGHTS',
      error: 'Oops there was an error!'
    }
    const initState = {
      isFetching : true,
      items: [
        {name:'toto', access:true},
        {name:'titi', access:false}
      ]
    }
    const expectedState = {
      isFetching : false,
      items: [
        {name:'toto', access:true},
        {name:'titi', access:false}
      ]
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
