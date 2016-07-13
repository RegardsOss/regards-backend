import { expect } from 'chai'
import { Action } from 'redux'
import reducer from '../../../../scripts/adminApp/modules/ui/reducer'

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
    let initState = {

    }
    const expectedState = {
      selectedProjectAdminId: 'toto'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
    initState = {
      selectedProjectAdminId: 'titi'
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should show the project configuration view and hide project admin configuration view', () => {
    const action = {
      type: 'SHOW_PROJECT_CONFIGURATION'
    }
    const initState = {
      projectConfigurationIsShown: false,
      projectAdminConfigurationIsShown: true
    }
    const expectedState = {
      projectConfigurationIsShown: true,
      projectAdminConfigurationIsShown: false
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should hide the project configuration view', () => {
    const action = {
      type: 'HIDE_PROJECT_CONFIGURATION'
    }
    const initState = {
      projectConfigurationIsShown: true,
    }
    const expectedState = {
      projectConfigurationIsShown: false,
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should show the project admin configuration view and hide project configuration view', () => {
    const action = {
      type: 'SHOW_PROJECT_ADMIN_CONFIGURATION'
    }
    const initState = {
      projectConfigurationIsShown: true,
      projectAdminConfigurationIsShown: false
    }
    const expectedState = {
      projectConfigurationIsShown: false,
      projectAdminConfigurationIsShown: true
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should hide the project admin configuration view', () => {
    const action = {
      type: 'HIDE_PROJECT_ADMIN_CONFIGURATION'
    }
    const initState = {
      projectAdminConfigurationIsShown: true,
    }
    const expectedState = {
      projectAdminConfigurationIsShown: false,
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
