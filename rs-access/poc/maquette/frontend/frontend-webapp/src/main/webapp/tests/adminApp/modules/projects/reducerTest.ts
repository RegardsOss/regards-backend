import { expect } from 'chai'
import { Action } from 'redux'
import reducer from '../../../../scripts/adminApp/modules/projects/reducer'
import { ProjectAction } from '../../../../scripts/adminApp/modules/projects/actions'

describe('[ADMIN APP] Testing projects reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({
      isFetching : false,
      items: {},
      ids: [],
      lastUpdate: ''
    })
  })

  it('should handle fetch request', () => {
    const action: Action = {
      type: 'PROJECTS_REQUEST'
    }
    const initState = {
      isFetching : false
    }
    const expectedState = {
      isFetching : true
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle fetch success', () => {
    const action = {
      type: 'PROJECTS_SUCESS',
      payload: {
        entities: {
          projects: {
            0: {id:0, name:"cdpp"},
            1: {id:1, name:"ssalto"}
          }
        },
        result: [0, 1]
      }
    }
    const initState = {
      isFetching : true,
      items: {},
      ids: Array(),
      lastUpdate: ''
    }
    const expectedState = {
      isFetching : false,
      items: {
        0: {id:0, name:"cdpp"},
        1: {id:1, name:"ssalto"}
      },
      ids: [0,1],
      lastUpdate: ''
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle fetch failure', () => {
    const action = {
      type: "PROJECTS_FAILURE",
      error: "Oops there was an error!"
    }
    const initState = {
      isFetching : true,
      items: {},
      ids: Array(),
      lastUpdate: ''
    }
    const expectedState = {
      isFetching : false,
      items: {},
      ids: Array(),
      lastUpdate: ''
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should create a project', () => {
    const action: ProjectAction = {
      type: 'ADD_PROJECT',
      id: 'idofthesecondproject',
      name: 'ssalto'
    }
    const initState = {
      items: {
        'idofthefirstproject': { name: 'cdpp', links: Array() },
      },
      ids: ['idofthefirstproject']
    }
    const expectedState = {
      items: {
        'idofthefirstproject': { name: 'cdpp', links: Array() },
        'idofthesecondproject': { name: 'ssalto', links: Array() },
      },
      ids: ['idofthefirstproject', 'idofthesecondproject']
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should delete a project', () => {
    const action = {
      type: 'DELETE_PROJECT',
      id: 'idofthesecondproject'
    }
    const initState = {
      items: {
        'idofthefirstproject': { name: 'cdpp', links: Array() },
        'idofthesecondproject': { name: 'ssalto', links: Array() },
      },
      ids: ['idofthefirstproject', 'idofthesecondproject']
    }
    const expectedState = {
      items: {
        'idofthefirstproject': { name: 'cdpp', links: Array() },
      },
      ids: ['idofthefirstproject']
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
