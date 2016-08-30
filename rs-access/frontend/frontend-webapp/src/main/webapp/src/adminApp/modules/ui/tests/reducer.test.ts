import { expect } from "chai"
import reducer from "../reducer"

describe('[ADMIN APP] Testing ui reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({})
  })

  it('should record which project is selected', () => {
    const action = {
      type: 'SELECT_PROJECT',
      id: 'cdpp'
    }
    let initState = {}
    const expectedState = {
      selectedProjectId: 'cdpp'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
    initState = {
      selectedProjectId: 'ssalto'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should record which user was clicked', () => {
    const action = {
      type: 'SELECT_PROJECT_ADMIN',
      id: 'toto'
    }
    let initState = {}
    const expectedState = {
      selectedProjectAdminId: 'toto'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
    initState = {
      selectedProjectAdminId: 'titi'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
