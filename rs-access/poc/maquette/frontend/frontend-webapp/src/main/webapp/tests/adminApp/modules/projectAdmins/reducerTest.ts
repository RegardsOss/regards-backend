import { expect } from 'chai'
import { Action } from 'redux'
import reducer from '../../../../scripts/adminApp/modules/projectAdmins/reducer'

describe('[ADMIN APP] Testing users reducer', () => {

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
      type: 'PROJECT_ADMIN_REQUEST'
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
      type: 'PROJECT_ADMIN_SUCESS',
      payload: {
        entities: {
          projectAdmins: {
            0: {id:0, name:"Toto"},
            1: {id:1, name:"Titi"}
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
        0: {id:0, name:"Toto"},
        1: {id:1, name:"Titi"}
      },
      ids: [0,1],
      lastUpdate: ''
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should handle fetch failure', () => {
    const action = {
      type: "PROJECT_ADMIN_FAILURE",
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

  it('should update a user', () => {
    const action = {
      type: 'UPDATE_PROJECT_ADMIN',
      id: 'Toto',
      payload: {
        name: 'TOTO',
        projects: [2]
      }
    }
    const initState = {
      items: {
        Toto: {name: 'Toto', projects: [0,1]},
        Titi: {name: 'Titi', projects: Array()}
      },
      ids: ['Toto', 'Titi']
    }
    const expectedState = {
      items: {
        Toto: {name: 'TOTO', projects: [2]},
        Titi: {name: 'Titi', projects: Array()}
      },
      ids: ['Toto', 'Titi']
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should create a user', () => {
    const action = {
      type: 'CREATE_PROJECT_ADMIN',
      id: 'Titi',
      payload: {
        name: 'Titi',
        projects: [0,1]
      }
    }
    const initState = {
      items: {
        Toto: {name: 'Toto', projects: Array()}
      },
      ids: ['Toto']
    }
    const expectedState = {
      items: {
        Toto: {name: 'Toto', projects: Array()},
        Titi: {name: 'Titi', projects: [0,1]}
      },
      ids: ['Toto', 'Titi']
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

  it('should delete a user', () => {
    const action = {
      type: 'DELETE_PROJECT_ADMIN',
      id: 'Titi'
    }
    const initState = {
      items: {
        Toto: {name: 'Toto', projects: [0,1]},
        Titi: {name: 'Titi', projects: Array()}
      },
      ids: ['Toto', 'Titi']
    }
    const expectedState = {
      items: {
        Toto: {name: 'Toto', projects: [0,1]},
      },
      ids: ['Toto']
    }
    expect(reducer(initState, action)).to.eql(expectedState)
  })

})
