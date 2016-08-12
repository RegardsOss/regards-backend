var configureMockStore = require('redux-mock-store')
var { apiMiddleware } = require('redux-api-middleware')
import thunk from 'redux-thunk'
import { expect } from 'chai' // You can use any testing library
import { Action, AnyMeta, TypedMeta, isFSA, isError } from 'flux-standard-action'

import { SET_LOCALE, SET_LOCALE_MSG, updateLocale } from '../../../src/common/i18n/I18nActions'

const middlewares = [ thunk, apiMiddleware ]
const mockStore = configureMockStore(middlewares)

describe('[COMMON] Testing i18n actions', () => {

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it('Test actions creation for internazionalisation language change', () => {

    const store = mockStore({
      common: {
        i18n: {
          locale: 'fr',
          messages: [{
            messagesDir: '../tests/common/i18n/messages/test1',
            messages: { 'message1':'premier message' }
          },{
            messagesDir: '../tests/common/i18n/messages/test2',
            messages: { 'message2':'deuxieme message' }
          }]
        }
      }
   });

    const setLocaleAction: any = {
      type: SET_LOCALE,
      locale: 'en'
    }

    const setLocaleMessage: any = {
      type: SET_LOCALE_MSG,
      messagesDir: '../tests/common/i18n/messages/test1',
      messages: { 'message1':'first message' }
    }

    const setLocaleMessage2: any = {
      type: SET_LOCALE_MSG,
      messagesDir: '../tests/common/i18n/messages/test2',
      messages: { 'message2':'second message' }
    }

    const expectedActions = [ setLocaleAction, setLocaleMessage, setLocaleMessage2 ]

    return store.dispatch(updateLocale('en'))
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

})
