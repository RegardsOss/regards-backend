import { expect } from "chai"
import { Action } from "redux"
import reducer from "../reducer"
import {
  ProjectAction,
  PROJECTS_REQUEST, PROJECTS_FAILURE, PROJECTS_SUCCESS,
  CREATE_PROJECT_REQUEST, CREATE_PROJECT_FAILURE, CREATE_PROJECT_SUCCESS,
  DELETE_PROJECT_REQUEST, DELETE_PROJECT_SUCCESS, DELETE_PROJECT_FAILURE
} from "../actions"

describe('[ADMIN APP] Testing projects reducer', () => {

  it('should return the initial state', () => {
    expect(reducer(undefined, {})).to.eql({
      isFetching: false,
      items: {},
      ids: [],
      lastUpdate: ''
    })
  })

  describe('GET /projects calls', () => {

    it('should handle fetch request', () => {
      const action: Action = {
        type: PROJECTS_REQUEST
      }
      const initState = {
        isFetching: false
      }
      const expectedState = {
        isFetching: true
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle fetch success', () => {
      const action = {
        type: PROJECTS_SUCCESS,
        payload: {
          entities: {
            projects: {
              0: {id: 0, name: "cdpp"},
              1: {id: 1, name: "ssalto"}
            }
          },
          result: [0, 1]
        }
      }
      const initState = {
        isFetching: true,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {
          0: {id: 0, name: "cdpp"},
          1: {id: 1, name: "ssalto"}
        },
        ids: [0, 1],
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle fetch failure', () => {
      const action = {
        type: PROJECTS_FAILURE,
        error: "Oops there was an error!"
      }
      const initState = {
        isFetching: true,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

  })

  describe('POST /projects calls', () => {

    it('should handle create request', () => {
      const action: Action = {
        type: CREATE_PROJECT_REQUEST
      }
      const initState = {
        isFetching: false
      }
      const expectedState = {
        isFetching: true
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle create success', () => {
      const action = {
        type: CREATE_PROJECT_SUCCESS,
        payload: {
          entities: {
            projects: {
              3: {id: 3, name: "newProject"}
            }
          },
          result: [3]
        }
      }
      const initState = {
        isFetching: true,
        items: {
          0: {id: 0, name: "cdpp"},
          1: {id: 1, name: "ssalto"}
        },
        ids: [0, 1],
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {
          0: {id: 0, name: "cdpp"},
          1: {id: 1, name: "ssalto"},
          3: {id: 3, name: "newProject"}
        },
        ids: [0, 1, 3],
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle create failure', () => {
      const action = {
        type: CREATE_PROJECT_FAILURE,
        error: "Oops there was an error!"
      }
      const initState = {
        isFetching: true,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

  })

  describe('DELETE /projects/{id} calls', () => {

    it('should handle delete request', () => {
      const action: Action = {
        type: DELETE_PROJECT_REQUEST
      }
      const initState = {
        isFetching: false
      }
      const expectedState = {
        isFetching: true
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle delete success', () => {
      const action = {
        type: DELETE_PROJECT_SUCCESS,
        payload: {
          entities: {
            projects: {
              3: {id: 3, name: "newProject"}
            }
          },
          result: [3]
        }
      }
      const initState = {
        isFetching: true,
        items: {
          0: {id: 0, name: "cdpp"},
          1: {id: 1, name: "ssalto"},
          3: {id: 3, name: "newProject"}
        },
        ids: [0, 1, 3],
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {
          0: {id: 0, name: "cdpp"},
          1: {id: 1, name: "ssalto"},
        },
        ids: [0, 1],
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

    it('should handle delete failure', () => {
      const action = {
        type: DELETE_PROJECT_FAILURE,
        error: "Oops there was an error!"
      }
      const initState = {
        isFetching: true,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      const expectedState = {
        isFetching: false,
        items: {},
        ids: Array(),
        lastUpdate: ''
      }
      expect(reducer(initState, action)).to.eql(expectedState)
    })

  })
})
